package com.dc.tools.timer;

import cn.hutool.core.date.SystemClock;
import com.google.common.collect.Lists;
import lombok.Getter;
import lombok.ToString;
import org.jctools.queues.MpscUnboundedArrayQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.dc.tools.common.utils.NumberPowerUtils.roundToPowerOfTwo;

/**
 * 多层级时间轮实现
 *
 * <p>
 * 一共划分为6个层级，由于caffeine默认支持的最低层级为1024ms，在计算数据时会有部分延迟，所以最低层级下降为16ms，保证任务延迟浮动在0-16ms之间
 * </p>
 *
 * @author zy
 * @apiNote Change from caffeine
 */
//TODO: 是否将每个wheelBucket中的mpsc队列迁移到上层由上层统一负责添加迁移，这样会降低部分性能但是对于常用场景已经足够使用
public class DelayWheel {

    private static final Logger log = LoggerFactory.getLogger(DelayWheel.class);

    /*
     * A timer wheel [1] stores timer events in buckets on a circular buffer. A bucket represents a
     * coarse time span, e.g. one minute, and holds a doubly-linked list of events. The wheels are
     * structured in a hierarchy (million, seconds, minutes, hours, days) so that events scheduled in the
     * distant future are cascaded to lower buckets when the wheels rotate. This allows for events
     * to be added, removed, and expired in O(1) time, where expiration occurs for the entire bucket,
     * and penalty of cascading is amortized by the rotations.
     *
     * [1] Hashed and Hierarchical Timing Wheels
     * http://www.cs.columbia.edu/~nahum/w6998/papers/ton97-timing-wheels.pdf
     */

    /**
     * 拆分的多个时间论，以及每个时间轮中的bucket
     */
    private static final int[] WHEEL = new int[]{64, 64, 64, 32, 4, 1};


    /**
     * 每一层时间轮每一个bucket的间隔时间单位
     */
    private static final long[] WHEEL_INTERVAL;

    static {
        WHEEL_INTERVAL = new long[WHEEL.length];
        WHEEL_INTERVAL[0] = roundToPowerOfTwo(TimeUnit.MILLISECONDS.toMillis(16));
        WHEEL_INTERVAL[1] = roundToPowerOfTwo(TimeUnit.SECONDS.toMillis(1));
        WHEEL_INTERVAL[2] = roundToPowerOfTwo(TimeUnit.MINUTES.toMillis(1));
        WHEEL_INTERVAL[3] = roundToPowerOfTwo(TimeUnit.HOURS.toMillis(1));
        WHEEL_INTERVAL[4] = roundToPowerOfTwo(TimeUnit.DAYS.toMillis(1));
        WHEEL_INTERVAL[5] = roundToPowerOfTwo(TimeUnit.DAYS.toMillis(4));
    }

    /**
     * 每一层bucket 对齐的时间单位的低位为0数量，用于计算不同层时间轮中时间对应的ticks
     */
    private static final int[] WHEEL_SHIFT;

    static {
        WHEEL_SHIFT = new int[WHEEL.length];
        for (int i = 0; i < WHEEL.length; i++) {
            WHEEL_SHIFT[i] = Long.numberOfTrailingZeros(WHEEL_INTERVAL[i]);
        }
    }


    /**
     * 整体的时间轮，第一层数据代表时间轮的层级，第二层数据代表每一层时间轮中具体的bucket数量
     */
    private final WheelBucket[][] wheels;

    /**
     * 最后更新的时间
     */
    private volatile long lastUpdateTime;


    public DelayWheel() {
        wheels = new WheelBucket[WHEEL.length][0];
        for (int wheelIndex = 0; wheelIndex < WHEEL.length; wheelIndex++) {
            int wheelLength = WHEEL[wheelIndex];
            wheels[wheelIndex] = new WheelBucket[wheelLength];
            for (int bucketIndex = 0; bucketIndex < wheelLength; bucketIndex++) {
                wheels[wheelIndex][bucketIndex] = new WheelBucket();
            }
        }

        this.lastUpdateTime = SystemClock.now();
    }


    /**
     * 添加延迟任务
     *
     * @param taskName  任务名称
     * @param taskId    任务id
     * @param delayTime 延迟的时间
     */
    public void addTask(String taskName, long taskId, long delayTime) {
        WheelBucket bucket = findBucket(delayTime);
        Task delayTask = new Task(taskName, taskId, delayTime);
        bucket.addTask(delayTask);
    }


    /**
     * 时间轮转动，处理过期的任务。
     *
     * <p>该方法根据当前时间和上次更新时间，计算需要处理的bucket，并返回过期的任务列表。</p>
     *
     * @param nowTime 当前时间（以毫秒为单位）
     * @return 过期的任务信息列表
     */
    public List<Task> advance(long nowTime) {

        long previousTime = lastUpdateTime;
        this.lastUpdateTime = nowTime;

        List<Task> expireTasks = new ArrayList<>();

        for (int wheelIndex = 0; wheelIndex < WHEEL_SHIFT.length; wheelIndex++) {
            long prevIndex = previousTime >>> WHEEL_SHIFT[wheelIndex];
            long afterIndex = nowTime >>> WHEEL_SHIFT[wheelIndex];
            if (afterIndex - prevIndex <= 0) {
                break;
            }

            List<Task> wheelExpireTasks = expireTasks(wheelIndex, prevIndex, afterIndex);
            expireTasks.addAll(wheelExpireTasks);
        }


        return expireTasks;
    }

    /**
     * 查找并处理指定范围内的过期任务。
     *
     * <p>该方法根据给定的时间范围，查找并处理指定轮次中的过期任务。</p>
     *
     * @param wheelIndex 轮次索引
     * @param prevIndex  上次更新时间对应的bucket索引
     * @param afterIndex 当前时间对应的bucket索引
     * @return 过期的任务信息列表
     */
    private List<Task> expireTasks(int wheelIndex, long prevIndex, long afterIndex) {
        //过期的任务
        List<Task> expireTasks = Lists.newArrayList();
        WheelBucket[] buckets = wheels[wheelIndex];
        int wheelMask = WHEEL[wheelIndex] - 1;
        int startIndex = (int) (prevIndex & wheelMask);
        int step = (int) Math.min(afterIndex - prevIndex + 1, WHEEL[wheelIndex]);

        for (int i = 0; i < step; i++) {
            int bucketIndex = startIndex & wheelMask;
            WheelBucket bucket = buckets[bucketIndex];
            List<Task> tasks = bucket.reset();
            for (Task task : tasks) {
                //如果差值大于16ms，则再次投递到时间轮，否则不再进行投递，避免下层时间轮转动导致的问题
                if (task.delayTime - lastUpdateTime > WHEEL_INTERVAL[0]) {
                    log.debug("Task: {}, lastUpdateTime: {}", task.delayTime - SystemClock.now(), lastUpdateTime);
                    addTask(task.taskName, task.taskId, task.delayTime);
                    continue;
                }

                //将已经过期的任务既不需要再被投递到时间轮中的任务，直接添加过期任务集合中
                expireTasks.add(task);
            }
            startIndex++;
        }
        return expireTasks;
    }


    /**
     * To determine the index of the bucket in the delay wheel where a task with a given delay time should be placed
     *
     * <p>
     * <b>1、Calculate Interval:</b>
     *  <ul>
     *   <li>
     *     Compute the interval between the current time (lastUpdateTime) and the task's delay time.
     *   </li>
     *  </ul>
     * <p>
     *   <b>2、Determine Wheel Level:</b>
     *  <ul>
     *   <li>Iterate through the predefined intervals (WHEEL_INTERVAL) to find the appropriate wheel level</li>
     *   <li>If the delay time is less than the interval of the next wheel level, determine the bucket index within the current wheel level.</li>
     *  </ul>
     * <p>
     * <b>3、Return Index:</b>
     * <ul>
     *  <li>Combine the wheel index and bucket index into a single integer value using bitwise operations</li>
     *  <li>If the delay time exceeds all predefined intervals, place it in the last wheel level.</li>
     * </ul>
     * </p>
     *
     * @param delayTime The delay time for the task.
     */
    private WheelBucket findBucket(long delayTime) {
        int index = findBucketIndex(delayTime);
        int wheelIndex = index >> 16;
        int bucketIndex = index & ~(-1 << 16);
        return wheels[wheelIndex][bucketIndex];
    }

    /**
     * 返回延迟时间对应的时间轮索引。
     *
     * <p>该方法根据给定的延迟时间，计算其对应的时间轮索引。</p>
     *
     * @param delayTime 延迟时间（以毫秒为单位）
     * @return 返回一个init类型的值，高16位表示wheelIndex，低16bit表示bucketIndex
     */
    public int findBucketIndex(long delayTime) {

        //计算时间间隔
        long interval = delayTime - lastUpdateTime;

        for (int wheelIndex = 0; wheelIndex < WHEEL_INTERVAL.length - 1; wheelIndex++) {
            if (interval < WHEEL_INTERVAL[wheelIndex + 1]) {
                long bucket = delayTime >>> WHEEL_SHIFT[wheelIndex];
                long bucketIndex = bucket & (WHEEL[wheelIndex] - 1);
                return (int) (wheelIndex << 16 | bucketIndex);
            }
        }

        return (WHEEL.length - 1) << 16;
    }


    /**
     * To find the earliest time at which a task in the delay wheel will expire. This method helps in determining the next tick or interval when the delay wheel should be advanced to process expiring task
     *
     * <p>
     * <p>
     * <b>1、Iterate Through Wheels:</b>
     *  <ul>
     *      <li>Loop through each level of the delay wheel from the finest-grained (smallest interval) to the coarsest-grained (largest interval)</li>
     *  </ul>
     * </p>
     *
     * <p>
     * <b>2、Determine Bucket Index:</b>
     * <ul>
     *   <li>For each wheel level, calculate the bucket index based on the current time (lastUpdateTime) and the wheel's interval</li>
     * </ul>
     * </p>
     *
     * <p>
     * <b>3、Check Buckets for Tasks:</b>
     * <ul>
     *   <li>For each bucket in the current wheel level, check if it contains any tasks (hasNodes())</li>
     * </ul>
     * </p>
     * <p>
     * <p>
     * <b> 4、Calculate Delay:</b>
     * <ul>
     *   <li>If a bucket contains tasks, calculate the delay until the next tick for that bucket</li>
     *   <li>Adjust the delay if necessary by comparing with parent wheel levels to ensure the correct minimum delay is returned.</li>
     * </ul>
     * </p>
     * <p>
     * <b>5、Return Minimum Delay:</b>
     * <ul>
     *   <li>Return the smallest delay found across all wheel levels and buckets. If no tasks are found, return -1</li>
     * </ul>
     * </p>
     * </p>
     *
     * @return Returns the delay (in milliseconds) until the earliest expiring task. If no tasks are present, it returns -1.
     */
    public long findEarliestTime() {
        for (int wheelIndex = 0; wheelIndex < WHEEL.length; wheelIndex++) {
            //当前时间每个轮次的间隔大小
            long wheelInterval = WHEEL_INTERVAL[wheelIndex];
            long intervalMask = wheelInterval - 1;

            //当前时间轮bucket的长度
            int bucketLength = WHEEL[wheelIndex];
            int bucketMask = bucketLength - 1;
            WheelBucket[] wheelBuckets = wheels[wheelIndex];
            long bucketTicks = lastUpdateTime >>> WHEEL_SHIFT[wheelIndex];
            int startIndex = (int) (bucketTicks & bucketMask);
            long endIndex = startIndex + bucketLength;

            for (int index = startIndex; index < endIndex; index++) {
                int bucketIndex = index & bucketMask;
                WheelBucket wheelBucket = wheelBuckets[bucketIndex];
                if (!wheelBucket.hasNodes()) {
                    continue;
                }

                long step = index - startIndex;

                long interval = lastUpdateTime & intervalMask;

//                log.warn("step: {}, startIndex: {}, stopIndex: {}, index: {}, interval: {}, wheelIndex: {}",
//                        step, startIndex, endIndex, index, interval, wheelIndex);

                //计算需要等待的延迟时间
                //(lastUpdateTime & intervalMask) 是需要计算离这个bucket结束还有多久
                long delay = ((step) << WHEEL_SHIFT[wheelIndex]) - (lastUpdateTime & intervalMask);

                //如果 delay < 0 则计算bucket结束的时间
                delay = (delay > 0) ? delay : WHEEL_INTERVAL[wheelIndex] - interval;

                //计算父层级时间论中下一个tick的过期时间与当前的tick时间进行对比，判断哪个更早
                for (int k = wheelIndex + 1; k < WHEEL.length; k++) {
                    long parentDelay = Long.MAX_VALUE;
                    //计算lastUpdateTime对应parent的ticks
                    long parentTicks = lastUpdateTime >>> WHEEL_SHIFT[k];
                    //计算bucket数量的mask
                    long parentMask = WHEEL[k] - 1;
                    //计算interval的mask
                    long parentIntervalMask = WHEEL_INTERVAL[k] - 1;
                    //计算parentTicks对应的bucketIndex是否包含任务
                    int parentBucketIndex = (int) ((parentTicks + 1) & parentMask);
                    WheelBucket parentBucket = wheels[k][parentBucketIndex];
                    if (parentBucket.hasNodes()) {
                        parentDelay = WHEEL_INTERVAL[k] - (lastUpdateTime & parentIntervalMask);
                    }


                    delay = Math.min(delay, parentDelay);
                }

                return delay;
            }
        }

        return -1;
    }


    private static class WheelBucket {

        /**
         * 用于存储所有的延迟任务
         */
        private volatile MpscUnboundedArrayQueue<Task> tasks;

        private final AtomicBoolean initialized = new AtomicBoolean(false);

        /**
         * lazy to init wheel buckets，and add task to wheel buckets
         *
         * @param task 需要添加的任务
         */
        void addTask(Task task) {
            //初始化节点
            for (; ; ) {
                if (tasks != null) {
                    break;
                }
                if (initialized.compareAndSet(false, true)) {
                    tasks = new MpscUnboundedArrayQueue<>(8);
                }
            }

            tasks.relaxedOffer(task);
        }


        /**
         * 重置bucket并返回过期任务列表。这里通过snapshotSize来获取当前队列中的任务信息，保证并发安全
         *
         * @return 过期任务列表
         */
        synchronized List<Task> reset() {
            //如果task还未初始化或者 为空则直接返回空
            if (tasks == null || tasks.isEmpty()) {
                return Collections.emptyList();
            }

            int snapshotSize = tasks.size();
            List<Task> expireTasks = Lists.newArrayList();
            tasks.drain(expireTasks::add, snapshotSize);
            return expireTasks;
        }

        /**
         * 检查bucket中是否有任务。
         *
         * @return 如果有任务返回true，否则返回false
         */
        public boolean hasNodes() {
            return tasks != null && !tasks.isEmpty();
        }

        @Override
        public String toString() {
            return "WheelBucket{" +
                    "tasks=" + tasks +
                    ", initialized=" + initialized +
                    '}';
        }
    }

    @ToString
    public static class Task {

        /**
         * 任务名称
         */
        private final String taskName;

        /**
         * 任务id
         */
        @Getter
        private final Long taskId;

        /**
         * 过期时间
         */
        private final long delayTime;

        /**
         * 构造函数，创建一个新的延迟任务。
         *
         * @param taskName  任务名称
         * @param taskId    任务id
         * @param delayTime 过期时间（以毫秒为单位）
         */
        private Task(String taskName, Long taskId, long delayTime) {
            this.taskName = taskName;
            this.delayTime = delayTime;
            this.taskId = taskId;
        }
    }


}
