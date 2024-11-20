package com.dc.pool.thread;

import com.codahale.metrics.*;
import com.dc.tools.common.utils.SystemClock;

import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Supplier;

import static com.codahale.metrics.MetricRegistry.name;

/**
 * 线程池统计信息
 *
 * @author zy
 */
public class PoolStates {

    private final MetricRegistry registry;

    private final ThreadPoolExecutor poolExecutor;

    private final Counter taskCounter;

    private final Gauge<Integer> queueGauge;

    private final Meter tasksMeter;

    private final Gauge<Integer> activePoolSize;

    private final Gauge<Integer> poolSize;

    private final Timer taskTimer;


    private SlidingTimeWindow timeWindow;

    private final Counter rejectCounter;

    /**
     *
     * @param poolName 线程池名称
     * @param registry 指标
     * @param windowInterval 统计的窗口间隔
     * @param executor 线程池
     */
    public PoolStates(String poolName, MetricRegistry registry, long windowInterval, ThreadPoolExecutor executor) {
        this.registry = registry;
        this.poolExecutor = executor;
        this.taskCounter = this.registry.register(name(poolName, "tasks"), new Counter());
        this.queueGauge = this.registry.register(name(poolName, "queue size"), new Gauge<Integer>() {
            @Override
            public Integer getValue() {
                return executor.getQueue().size();
            }
        });

        this.tasksMeter = this.registry.register(name(poolName, "1min inc ratio"), new Meter());
        this.activePoolSize = this.registry.register(name(poolName, "active pool thread"), new Gauge<Integer>() {
            @Override
            public Integer getValue() {
                return poolExecutor.getActiveCount();
            }
        });

        this.poolSize = this.registry.register(name(poolName, "actual pool size"), new Gauge<Integer>() {
            @Override
            public Integer getValue() {
                return poolExecutor.getPoolSize();
            }
        });

        this.taskTimer = this.registry.register(name(poolName, "task execute time"), new Timer());
        this.rejectCounter = this.registry.register(name(poolName, "reject counter"), new Counter());
        this.timeWindow = new SlidingTimeWindow(queueGauge::getValue, windowInterval);

    }

    public void incAdd() {
        taskCounter.inc();
        tasksMeter.mark();
        timeWindow.incAdd();
    }

    public void incComplete() {
        timeWindow.incHandle();
    }

    public void incReject() {
        rejectCounter.inc();
        timeWindow.incReject();
    }

    public long completeTasks() {
        return poolExecutor.getCompletedTaskCount();
    }

    /**
     * 执行任务的增长率
     */
    public double incRatio() {
        return tasksMeter.getOneMinuteRate();
    }

    public long activeTasks() {
        return queueGauge.getValue();
    }

    public int activePoolSize() {
        return activePoolSize.getValue();
    }

    public int poolSize() {
        return poolSize.getValue();
    }

    public int coreSize() {
        return poolExecutor.getCorePoolSize();
    }

    public int maxPoolSize() {
        return poolExecutor.getMaximumPoolSize();
    }


    public PoolStates recordTime(long time) {
        taskTimer.update(time, TimeUnit.MILLISECONDS);
        return this;
    }

    public double taskTime() {
        return taskTimer.getSnapshot().getMean();
    }

    /**
     * 一分钟内的消费数据
     */
    public double oneMinuteRatio() {
        return timeWindow.ratio();
    }

    static class SlidingTimeWindow {

        /**
         * 窗口开始前 队列的大小
         */
        private volatile int snapshotQueueSize;


        /**
         * 时间窗口内一共添加的任务
         */
        private final LongAdder addCounter = new LongAdder();

        /**
         * 时间窗口内消费的数据量
         */
        private final LongAdder handleCounter = new LongAdder();

        private final LongAdder rejectCounter = new LongAdder();

        private volatile long lastUpdateTime;

        private static final AtomicLongFieldUpdater<SlidingTimeWindow> UPDATER =
                AtomicLongFieldUpdater.newUpdater(SlidingTimeWindow.class, "lastUpdateTime");

        private final long startTime;

        private final Supplier<Integer> queueSupplier;

        private final long windowInterval;

        SlidingTimeWindow(Supplier<Integer> queueSupplier, long windowInterval) {
            this.queueSupplier = queueSupplier;
            this.startTime = SystemClock.now();
            this.windowInterval = windowInterval;
        }

        public boolean isDep() {
            long nowTime = SystemClock.now();
            return (nowTime - startTime) - (lastUpdateTime >> 1) > windowInterval;
        }

        public void reset() {
            for (; ; ) {
                long time = lastUpdateTime;
                if (!isDep()) {
                    break;
                }
                if ((lastUpdateTime & 1) == 0 && UPDATER.compareAndSet(this, time, time | 1)) {
                    this.addCounter.reset();
                    this.handleCounter.reset();
                    this.lastUpdateTime = (SystemClock.now() - startTime) << 1;
                    this.snapshotQueueSize = queueSupplier.get();
                }
            }


        }

        public void incAdd() {
            if (isDep()) {
                reset();
            }

            addCounter.increment();
        }

        public void incReject() {
            if (isDep()) {
                reset();
            }

            rejectCounter.increment();
        }

        public void incHandle() {
            if (isDep()) {
                reset();
            }

            handleCounter.increment();
        }

        public double ratio() {
            //这个时候可能存在替换数据的行为所以需要等待直到数据替换完成
            while ((lastUpdateTime & 1) != 0) {
                Thread.yield();
            }

            long taskCount = snapshotQueueSize + addCounter.sum() + rejectCounter.sum();
            return handleCounter.sum() / taskCount == 0 ? 1 : taskCount;
        }
    }

}
