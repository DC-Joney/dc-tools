package com.dc.pool.thread;

import com.codahale.metrics.Timer;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * A {@link java.util.concurrent.ExecutorService} that with a timer metric
 * which aggregates timing durations and provides duration statistics.
 *
 * @author jiachun.fjc
 * @apiNote Fork from <a href="https://github.com/sofastack/sofa-jraft">Soft-Jraft</a>
 */
public class MetricThreadPoolExecutor extends LogThreadPoolExecutor {

    public MetricThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit,
                                    BlockingQueue<Runnable> workQueue, String name) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, name);
    }

    public MetricThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit,
                                    BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory, String name) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, name);
    }

    public MetricThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit,
                                    BlockingQueue<Runnable> workQueue, RejectedExecutionHandler handler, String name) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, handler, name);
    }

    public MetricThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit,
                                    BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory,
                                    RejectedExecutionHandler handler, String name) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler, name);
    }

    @Override
    protected void beforeExecute(Thread t, Runnable r) {
        super.beforeExecute(t, r);
        try {
            ThreadPoolMetricRegistry.timerThreadLocal() //
                    .set(ThreadPoolMetricRegistry.metricRegistry().timer("threadPool." + getName()).time());
        } catch (final Throwable ignored) {
            // ignored
        }
    }

    @Override
    protected void afterExecute(Runnable r, Throwable t) {
        super.afterExecute(r, t);
        try {
            final ThreadLocal<Timer.Context> tl = ThreadPoolMetricRegistry.timerThreadLocal();
            final Timer.Context ctx = tl.get();
            if (ctx != null) {
                ctx.stop();
                tl.remove();
            }
        } catch (final Throwable ignored) {
            // ignored
        }
    }
}
