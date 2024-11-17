package com.dc.tools.timer;

import com.dc.tools.common.utils.SystemClock;
import com.google.common.collect.Lists;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.jctools.queues.MpscUnboundedArrayQueue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class DelayWheel {


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
        WHEEL_INTERVAL[0] = power2(TimeUnit.MILLISECONDS.toMillis(16));
        WHEEL_INTERVAL[1] = power2(TimeUnit.SECONDS.toMillis(1));
        WHEEL_INTERVAL[2] = power2(TimeUnit.MINUTES.toMillis(1));
        WHEEL_INTERVAL[3] = power2(TimeUnit.HOURS.toMillis(1));
        WHEEL_INTERVAL[4] = power2(TimeUnit.DAYS.toMillis(1));
        WHEEL_INTERVAL[5] = power2(TimeUnit.DAYS.toMillis(4));
    }

    private static final int[] WHEEL_SHIFT;

    static {
        WHEEL_SHIFT = new int[WHEEL.length];
        for (int i = 0; i < WHEEL.length; i++) {
            WHEEL_SHIFT[i] = Long.numberOfTrailingZeros(WHEEL_INTERVAL[i]);
        }
    }


    static long power2(long value) {
        return 1L << -(Long.numberOfLeadingZeros(value - 1));
    }


    /**
     * 整体的时间轮，第一层数据代表时间轮的层级，第二层数据代表每一层时间轮中具体的bucket数量
     */
    private final WheelBucket[][] wheels;

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
     * 时间轮转动
     *
     * @return 过期的任务信息
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

    private List<Task> expireTasks(int wheelIndex, long prevIndex, long afterIndex) {

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
                    //重新投递到时间轮
                    log.warn("Task: {}, lastUpdateTime: {}", task.delayTime, lastUpdateTime);
                    addTask(task.taskName, task.taskId, task.delayTime);
                    continue;
                }
                expireTasks.add(task);
            }
            startIndex++;
        }
        return expireTasks;
    }


    /**
     * 查找延时时间对应的时间轮位置
     *
     * @param delayTime 延迟时间
     */
    public WheelBucket findBucket(long delayTime) {
        int index = findBucketIndex(delayTime);
        int wheelIndex = index >> 16;
        int bucketIndex = index & ~(-1 << 16);
//        log.debug("添加的bucket位置为: wheelIndex: " + wheelIndex + ", bucketIndex: " + bucketIndex);
        return wheels[wheelIndex][bucketIndex];
    }

    /**
     * 返回延迟时间对应的时间轮索引
     *
     * @param delayTime 延迟时间
     */
    private int findBucketIndex(long delayTime) {

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
     * 查找最早过期的时间轮任务
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
                delay = (delay > 0) ? delay: WHEEL_INTERVAL[wheelIndex] - interval;

                //计算父层级时间论中下一个tick的过期时间与当前的tick时间进行对比，判断哪个更早
                for (int k = wheelIndex + 1; k < WHEEL.length; k++) {
                    long parentDelay = Long.MAX_VALUE;
                    long parentTicks = lastUpdateTime >>> WHEEL_SHIFT[k];
                    long parentMask = WHEEL[k] - 1;
                    int parentBucketIndex = (int) ((parentTicks + 1) & parentMask);
                    WheelBucket parentBucket = wheels[k][parentBucketIndex];
                    if (parentBucket.hasNodes()) {
                        parentDelay = WHEEL_INTERVAL[k] - (lastUpdateTime >>> WHEEL_SHIFT[k]);
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


        public synchronized List<Task> reset() {
            //如果task还未初始化或者 为空则直接返回空
            if (tasks == null || tasks.isEmpty()) {
                return Collections.emptyList();
            }

            int snapshotSize = tasks.size();
            List<Task> expireTasks = Lists.newArrayList();
            tasks.drain(expireTasks::add, snapshotSize);
            return expireTasks;
        }

        public boolean hasNodes() {
            return tasks != null && !tasks.isEmpty();
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

        private Task(String taskName, Long taskId, long delayTime) {
            this.taskName = taskName;
            this.delayTime = delayTime;
            this.taskId = taskId;
        }
    }


    public static void main(String[] args) {
        long l = TimeUnit.MINUTES.toMillis(1);
        System.out.println(l);
        System.out.println(power2(l));
        System.out.println(power2(l) / (double) l);

        long l3 = TimeUnit.MINUTES.toNanos(1);
        System.out.println(l3);
        System.out.println(power2(l3));
        System.out.println(power2(l3) / (double) l3);

        long l1 = TimeUnit.SECONDS.toNanos(1);
        System.out.println(l1);
        System.out.println(power2(l1));
        System.out.println(power2(l1) / (double) l1);

        long l2 = TimeUnit.SECONDS.toMillis(1);
        System.out.println(l2);
        System.out.println(power2(l2) / (double) l2);


        long l4 = TimeUnit.DAYS.toMillis(1);
        System.out.println(l4);
        System.out.println(power2(l4) / (double) l4);

        long l5 = TimeUnit.DAYS.toNanos(1);
        System.out.println(l5);
        System.out.println(power2(l5) / (double) l5);
    }

}
