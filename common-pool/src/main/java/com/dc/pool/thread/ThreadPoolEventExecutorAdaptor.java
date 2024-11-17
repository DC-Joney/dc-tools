package com.dc.pool.thread;

import com.dc.tools.common.annotaion.NonNull;
import io.netty.util.concurrent.AbstractEventExecutor;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.Future;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 用于包装 Netty的 {@link EventExecutor}
 * @author zhangyang
 *
 * @apiNote Fork from <a href="https://github.com/sofastack/sofa-jraft">Soft-Jraft</a>
 */
public class ThreadPoolEventExecutorAdaptor extends AbstractEventExecutor {

    public ExecutorService deleget;

    public ThreadPoolEventExecutorAdaptor(ExecutorService executorService) {
        this.deleget = executorService;
    }

    @Override
    public void shutdown() {
        deleget.shutdown();
    }

    @Override
    public boolean inEventLoop(Thread thread) {
        return false;
    }

    @Override
    public boolean isShuttingDown() {
        return deleget.isShutdown();
    }

    @Override
    public Future<?> shutdownGracefully(long quietPeriod, long timeout, TimeUnit unit) {
        throw new UnsupportedOperationException("Can not support shutdownGracefully");
    }

    @Override
    public Future<?> terminationFuture() {
        throw new UnsupportedOperationException("Can not support get terminationFuture");
    }

    @Override
    public boolean isShutdown() {
        return deleget.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return deleget.isTerminated();
    }

    @Override
    public boolean awaitTermination(long timeout, @NonNull TimeUnit unit) throws InterruptedException {
        return deleget.awaitTermination(timeout, unit);
    }

    @Override
    public void execute(@NonNull Runnable command) {
        deleget.execute(command);
    }

}
