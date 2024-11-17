package com.dc.tools.common.thread;

import com.dc.tools.common.annotaion.NonNull;

import java.util.concurrent.*;

/**
 * 用于对ThreadPoolExecutor 做增强处理，实现可监听的FutureTask
 * @author zhangyang
 */
@SuppressWarnings("unchecked")
public class ListenableThreadPoolExecutor extends ThreadPoolExecutor {

    public ListenableThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit,
                                        BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory,RejectedExecutionHandler executionHandler) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, executionHandler);
    }

    public ListenableThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit,
                                        BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory);
    }

    public ListenableThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit,
                                        BlockingQueue<Runnable> workQueue) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
    }

    @Override
    public <T> FutureAdaptor<T> newTaskFor(Callable<T> callable) {
        return callable instanceof FutureAdaptor ? (FutureAdaptor<T>) callable : new FutureAdaptor<>(callable);
    }

    @Override
    public <T> FutureAdaptor<T> newTaskFor(Runnable runnable, T value) {
        return runnable instanceof FutureAdaptor ? (FutureAdaptor<T>) runnable : new FutureAdaptor<>(runnable, value);
    }

    public <T> FutureAdaptor<T> newTaskFor(Runnable runnable) {
        return runnable instanceof FutureAdaptor ? (FutureAdaptor<T>) runnable : new FutureAdaptor<>(runnable ,null);
    }


    @Override
    @SuppressWarnings("rawtypes")
    public void execute(@NonNull Runnable runnable) {
        runnable =  runnable instanceof FutureAdaptor ? (FutureAdaptor) runnable : new FutureAdaptor<>(runnable ,null);
        super.execute(runnable);
    }
}
