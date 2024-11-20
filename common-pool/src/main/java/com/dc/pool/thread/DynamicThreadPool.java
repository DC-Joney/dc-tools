package com.dc.pool.thread;

import com.codahale.metrics.MetricRegistry;
import com.dc.tools.common.annotaion.NonNull;
import com.dc.tools.common.utils.Assert;
import com.dc.tools.common.utils.MethodInvoker;
import com.dc.tools.common.utils.SystemClock;
import io.netty.util.concurrent.FastThreadLocal;
import lombok.RequiredArgsConstructor;

import javax.annotation.concurrent.NotThreadSafe;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;

/**
 * 动态线程池，依旧依赖于线程池自身的最大线程数和最小线程数
 * <ul>
 *     <li>1、改变原有的线程池策略，当检测到消费比例 < 我们设置的ratio时，会触发添加worker线程来解决下游消费慢问题,而避免导致的队列积压</li>
 *     <li>2、当有大量的拒绝任务执行时，会自动扩容maxPoolSize 到 maxWorkers</li>
 * </ul>
 * <p>
 *
 * @author zy
 */
//TODO: 1、整体代码性能需要优化，拒绝策略在执行时是否增加时间窗口概念，避免同一时间段内线程数暴增 <br/>
//      2、检测消费比例的窗口在 窗口切换时会导致消费比率清空，是否需要当前窗口部分周期内 继承上游窗口比例
@NotThreadSafe
public class DynamicThreadPool extends ThreadPoolExecutor {

    private MetricRegistry metricRegistry;

    private final PoolStates poolStates;

    private static final FastThreadLocal<Long> timeThreadLocal = new FastThreadLocal<>();

    private static final RejectedExecutionHandler EMPTY = (r, executor) -> {

    };

    private final MethodInvoker methodInvoker = new MethodInvoker();

    /**
     * 记录时间周期、时间周期内允许的最大worker增长数量
     */
    private volatile long state;

    private static final AtomicLongFieldUpdater<DynamicThreadPool> STATE_UPDATER
            = AtomicLongFieldUpdater.newUpdater(DynamicThreadPool.class, "state");

    /**
     * 触发扩容的周期, 会根据该周期计算消费的速率以及支持在该周期内最多扩容多少个worker线程
     *
     * @apiNote
     */
    private long windowInterval;

    /**
     * 周期内最多可以支持扩容多少个worker
     */
    private int windowMaxWorker;

    /**
     * 触发扩容worker时，队列的长度
     *
     * @apiNote 如果阻塞队列是有界阻塞队列，当队列长度满以后会自动触发线程扩容, 所以这里需要考虑队列的长度最高只能为队列长度的80%
     */
    private int queueSize;

    /**
     * 下游处理的速率 触发扩容的比例阈值
     */
    private double ratio;


    /**
     * 当队列的拒绝任务的数量
     */
    private long rejectCount;

    /**
     * 开始的时间
     */
    private final long startTime;

    /**
     * 允许扩容的最大worker的数量
     */
    private final int maxWorkers;


    private final AtomicInteger increment = new AtomicInteger();

    private static final int COUNT_SHIFT = 9;


    private static final int COUNT_MAGIC = ~(-1 << COUNT_SHIFT);


    public DynamicThreadPool(int corePoolSize, int maximumPoolSize,
                             long keepAliveTime, TimeUnit unit,
                             BlockingQueue<Runnable> workQueue,
                             long windowInterval,
                             ThreadFactory threadFactory,
                             RejectedExecutionHandler handler) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, EMPTY);
        this.poolStates = new PoolStates("", metricRegistry, windowInterval, this);
        this.startTime = SystemClock.now();
        this.maxWorkers = maximumPoolSize + corePoolSize;
        setRejectedExecutionHandler(new RejectHandlerWrapper(handler));
        initMethodInvoker();
    }

    public void setWindowMaxWorker(int windowMaxWorker) {
        Assert.isTrue(windowMaxWorker <= COUNT_MAGIC >>> 1, "window time increment maxWorkers must be <", COUNT_MAGIC >>> 1);
        this.windowMaxWorker = windowMaxWorker;
    }

    public void setWindowInterval(long interval) {
        this.windowInterval = interval;
    }

    public void setQueueSize(int queueSize) {
        this.queueSize = queueSize;
    }

    public void setRejectCount(long rejectCount) {
        this.rejectCount = rejectCount;
    }

    public void setRatio(double ratio) {
        this.ratio = ratio;
    }



    private void initMethodInvoker() {
        try {
            methodInvoker.setTargetMethod("addWorker");
            methodInvoker.setArguments(null, false);
            methodInvoker.setTargetClass(ThreadPoolExecutor.class);
            methodInvoker.setTargetObject(this);
            methodInvoker.prepare();
        } catch (Exception e) {
            throw new IllegalArgumentException("Cannot find  addWorker method from ThreadPoolExecutor");
        }
    }

    @Override
    public void execute(@NonNull Runnable command) {
        poolStates.incAdd();



        super.execute(command);
    }

    @Override
    protected void beforeExecute(Thread t, Runnable r) {
        //尝试去添加worker线程
        //TODO: 是否需要将tryAddWorker 移动到execute方法中
        tryAddWorker();
        timeThreadLocal.set(SystemClock.now());
        //有的任务执行时间过长，所以不被计算在最终结果
        poolStates.incComplete();
        super.beforeExecute(t, r);
    }

    private void tryAddWorker() {
        for (; ; ) {

            //如果触发的比例以及队列的数量不满足需求
            if (poolStates.oneMinuteRatio() >= ratio || getQueue().size() < queueSize) {
                break;
            }

            if (getPoolSize() < getCorePoolSize() || getPoolSize() >= maxWorkers) {
                break;
            }

            long s = state;

            long nowTime = SystemClock.now() - startTime;
            long lastTime = state >>> COUNT_SHIFT;
            long counter = (state & COUNT_MAGIC) >>> 1;
            //如果在当前的时间间隔内已经扩容到支持的最大worker数量
            if (counter >= windowMaxWorker && nowTime - lastTime <= windowInterval) {
                break;
            }

            if ((s & 1) == 0 && STATE_UPDATER.compareAndSet(this, s, s | 1)) {
                try {
                    //重置时间
                    if (nowTime - lastTime >= windowInterval) {
                        s = nowTime << COUNT_SHIFT;
                    }

                    //记录扩容的worker数量
                    s += 2;

                    //如果已经扩容到最大线程池还是无法解决问题，则继续膨胀
                    if (getPoolSize() >= getMaximumPoolSize() && getPoolSize() < maxWorkers) {
                        //设置最大线程池数量
                        setMaximumPoolSize(getMaximumPoolSize() + 1);
                    }

                    //如果线程池目前的处理小于 则
                    if (getPoolSize() >= getCorePoolSize()) {
                        methodInvoker.invoke();
                    }
                } catch (Exception e) {
                    //todo: log exception
                } finally {
                    //释放自旋锁
                    STATE_UPDATER.compareAndSet(this, s, s >> 1 << 1);
                }

                break;
            }

            //当前线程释放cpu时间片，避免过多的空自旋
            Thread.yield();

        }

    }


    @Override
    protected void afterExecute(Runnable r, Throwable t) {
        try {
            Long startTime = timeThreadLocal.get();
            //记录任务执行的时间
            poolStates.recordTime(SystemClock.now() - startTime);
        } finally {
            timeThreadLocal.remove();
        }

        super.afterExecute(r, t);
    }

    @RequiredArgsConstructor
    class RejectHandlerWrapper implements RejectedExecutionHandler {

        private final RejectedExecutionHandler delegate;


        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            DynamicThreadPool.this.poolStates.incReject();
            this.delegate.rejectedExecution(r, executor);
        }
    }

    public PoolStates getPoolStates() {
        return poolStates;
    }
}
