package com.dc.tools.timer;

import com.dc.tools.common.annotaion.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.Disposable;
import reactor.core.scheduler.Scheduler;

import java.util.concurrent.TimeUnit;

/**
 * 适配 {@link Scheduler} 类型的延迟任务执行器，底层通过 {@link Timer} 实现
 *
 * @author zy
 * @see Scheduler
 */
public class SchedulerTimer implements Scheduler {

    private static final Logger log = LoggerFactory.getLogger(SchedulerTimer.class);

    private final Timer timer;

    public SchedulerTimer(Timer timer) {
        this.timer = timer;
    }

    @Override
    @NonNull
    public Disposable schedule(@NonNull Runnable task, long delay, TimeUnit unit) {
        log.debug("Scheduling task delay is: {}", unit.toMillis(delay));
        RunnableTaskAdaptor adaptor = new RunnableTaskAdaptor(timer, task, unit.toMillis(delay) << 1);
        timer.addTask(adaptor);
        return adaptor;
    }

    @Override
    @NonNull
    public Disposable schedulePeriodically(@NonNull Runnable task, long initialDelay, long period, @NonNull TimeUnit unit) {
        log.debug("schedulePeriodically task delay is: {}, period is: {}", unit.toMillis(initialDelay), unit.toMillis(period));
        RunnableTaskAdaptor adaptor = new RunnableTaskAdaptor(timer, task, unit.toMillis(period) << 1 | 1);
        timer.addTask(adaptor, unit.toMillis(initialDelay), TimeUnit.MILLISECONDS);
        return adaptor;
    }


    @Override
    public void dispose() {
        log.debug("scheduler timer is stop");
        timer.stop();
    }

    @Override
    public void start() {
        log.debug("scheduler timer is start");
        timer.start();
    }

    @Override
    @NonNull
    public Disposable schedule(@NonNull Runnable task) {
        RunnableTaskAdaptor adaptor = new RunnableTaskAdaptor(timer, task, 0);
        timer.execute(task);
        return adaptor;
    }

    @Override
    public Worker createWorker() {
        return new TimerWorker(this);
    }

    public static class TimerWorker implements Worker {

        private final SchedulerTimer timer;

        public TimerWorker(SchedulerTimer timer) {
            this.timer = timer;
        }

        @Override
        @NonNull
        public Disposable schedule(@NonNull Runnable task) {
            return timer.schedule(task);
        }

        @Override
        @NonNull
        public Disposable schedule(@NonNull Runnable task, long delay, @NonNull TimeUnit unit) {
            return timer.schedule(task, delay, unit);
        }

        @Override
        @NonNull
        public Disposable schedulePeriodically(@NonNull Runnable task, long initialDelay, long period, @NonNull TimeUnit unit) {
            return timer.schedulePeriodically(task, initialDelay, period, unit);
        }

        @Override
        public void dispose() {
            timer.dispose();
        }
    }

    public static class RunnableTaskAdaptor extends FixTimeTask implements Disposable {

        private final Runnable runnable;

        private final long delayMillion;

        private final Timer timer;

        public RunnableTaskAdaptor(Timer timer, Runnable runnable, long delayMillion) {
            super("runnable timer");
            this.runnable = runnable;
            this.delayMillion = delayMillion;
            this.timer = timer;
        }


        @Override
        protected void runTask() {
            runnable.run();
        }

        @Override
        protected void afterExecute(Exception e) {
            if (e != null) {
                log.error("execute delay task failed, cause is: ", e);
            }

            if (delayMillion > 0 && (delayMillion & 1) == 1) {
                timer.addTask(this);
            }
        }

        @Override
        public void dispose() {
            cancel();
        }

        @Override
        protected long delayTime() {
            return delayMillion >>> 1;
        }
    }
}
