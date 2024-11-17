package com.dc.tools.trace;

import javafx.concurrent.Worker;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.lang.NonNull;
import reactor.core.Disposable;
import reactor.core.scheduler.Scheduler;

import java.util.concurrent.TimeUnit;

/**
 * 用于串联链路的 {@link Scheduler}
 *
 * @author zhangyang
 * @see reactor.core.scheduler.Schedulers
 * @see Scheduler
 * @see Worker
 * @see reactor.core.publisher.ParallelFlux
 */
@RequiredArgsConstructor
public class TraceScheduler implements Scheduler {

    private final Scheduler delegate;

    @Override
    public Disposable schedule(@NonNull Runnable task, long delay, @NonNull TimeUnit unit) {
        task = AsyncRunnable.async(task);
        return delegate.schedule(task, delay, unit);
    }

    @Override
    public Disposable schedulePeriodically(@NonNull Runnable task, long initialDelay, long period, @NonNull TimeUnit unit) {
        task = AsyncRunnable.async(task);
        return delegate.schedulePeriodically(task, initialDelay, period, unit);
    }

    @Override
    public long now(@NonNull TimeUnit unit) {
        return delegate.now(unit);
    }

    @Override
    public void dispose() {
        delegate.dispose();
    }

    @Override
    public void start() {
        delegate.start();
    }

    @Override
    public Disposable schedule(@NonNull Runnable task) {
        task = AsyncRunnable.async(task);
        return delegate.schedule(task);
    }

    @Override
    public Worker createWorker() {
        return new TraceWorker(delegate.createWorker());
    }

    /**
     * 创建可用于跟踪链路的任务调度器
     *
     * @param scheduler 任务调度器
     */
    public static TraceScheduler create(Scheduler scheduler) {
        return new TraceScheduler(scheduler);
    }

    @RequiredArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    private static class TraceWorker implements Worker {

        Worker delegate;

        @Override
        public Disposable schedule(@NonNull Runnable task, long delay, @NonNull TimeUnit unit) {
            task = AsyncRunnable.async(task);
            return delegate.schedule(task, delay, unit);
        }

        @Override
        public Disposable schedulePeriodically(@NonNull Runnable task, long initialDelay, long period, @NonNull TimeUnit unit) {
            task = AsyncRunnable.async(task);
            return delegate.schedulePeriodically(task, initialDelay, period, unit);
        }

        @Override
        public Disposable schedule(@NonNull Runnable task) {
            task = AsyncRunnable.async(task);
            return delegate.schedule(task);
        }

        @Override
        public void dispose() {
            delegate.dispose();
        }
    }
}
