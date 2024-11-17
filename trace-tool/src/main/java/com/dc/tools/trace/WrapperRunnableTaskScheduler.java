package com.dc.tools.trace;

import lombok.AllArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;

/**
 * 用于适配Spring的定时任务类，保证定时任务在执行时可以有自己的链路id，方便问题排查
 *
 * @author zhangyang
 */
public class WrapperRunnableTaskScheduler extends ThreadPoolTaskScheduler {


    @Override
    @NonNull
    protected ScheduledExecutorService createExecutor(int poolSize, @NonNull ThreadFactory threadFactory, @NonNull RejectedExecutionHandler rejectedExecutionHandler) {
        ScheduledExecutorService scheduledExecutorService = super.createExecutor(poolSize, threadFactory, rejectedExecutionHandler);
        return new TraceSchedulerThreadPoolExecutor(scheduledExecutorService);
    }

    @AllArgsConstructor
    private static class TraceSchedulerThreadPoolExecutor implements ScheduledExecutorService {

        private final ScheduledExecutorService delegate;

        @Override
        public ScheduledFuture<?> schedule(@NonNull Runnable command, long delay, @NonNull TimeUnit unit) {
            command = command instanceof AsyncTrace ? command : AsyncRunnable.async(command);
            return delegate.schedule(command, delay, unit);
        }

        @Override
        public <V> ScheduledFuture<V> schedule(@NonNull Callable<V> callable, long delay, @NonNull TimeUnit unit) {
            callable = callable instanceof AsyncTrace ? callable : AsyncCallable.async(callable);
            return delegate.schedule(callable, delay, unit);
        }

        @Override
        public ScheduledFuture<?> scheduleAtFixedRate(@NonNull Runnable command, long initialDelay, long period, @NonNull TimeUnit unit) {
            command = command instanceof AsyncTrace ? command : AsyncRunnable.async(command);
            return delegate.scheduleAtFixedRate(command, initialDelay, period, unit);
        }

        @Override
        public ScheduledFuture<?> scheduleWithFixedDelay(@NonNull Runnable command, long initialDelay, long delay, @NonNull TimeUnit unit) {
            command = command instanceof AsyncTrace ? command : AsyncRunnable.async(command);
            return delegate.scheduleWithFixedDelay(command, initialDelay, delay, unit);
        }

        @Override
        public void shutdown() {
            delegate.shutdown();
        }

        @Override
        public List<Runnable> shutdownNow() {
            return delegate.shutdownNow();
        }

        @Override
        public boolean isShutdown() {
            return delegate.isShutdown();
        }

        @Override
        public boolean isTerminated() {
            return delegate.isTerminated();
        }

        @Override
        public boolean awaitTermination(long timeout, @NonNull TimeUnit unit) throws InterruptedException {
            return delegate.awaitTermination(timeout, unit);
        }

        @Override
        public <T> Future<T> submit(@NonNull Callable<T> command) {
            command = command instanceof AsyncTrace ? command : AsyncCallable.async(command);
            return delegate.submit(command);
        }

        @Override
        public <T> Future<T> submit(@NonNull Runnable runnable, T result) {
            Callable<T> callable = Executors.callable(runnable, result);
            return submit(callable);
        }

        @Override
        public Future<?> submit(@NonNull Runnable command) {
            command = command instanceof AsyncTrace ? command : AsyncRunnable.async(command);
            return delegate.submit(command);
        }

        @Override
        public <T> List<Future<T>> invokeAll(@NonNull Collection<? extends Callable<T>> tasks) throws InterruptedException {
            return delegate.invokeAll(tasks);
        }

        @Override
        public <T> List<Future<T>> invokeAll(@NonNull Collection<? extends Callable<T>> tasks, long timeout, @NonNull TimeUnit unit) throws InterruptedException {
            return delegate.invokeAll(tasks, timeout, unit);
        }

        @Override
        public <T> T invokeAny(@NonNull Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
            return delegate.invokeAny(tasks);
        }

        @Override
        public <T> T invokeAny(@NonNull Collection<? extends Callable<T>> tasks, long timeout, @NonNull TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            return delegate.invokeAny(tasks, timeout, unit);
        }

        @Override
        public void execute(@NonNull Runnable command) {
            command = command instanceof AsyncTrace ? command : AsyncRunnable.async(command);
            delegate.execute(command);
        }
    }
}
