package com.dc.tools.timer;

import cn.hutool.core.date.SystemClock;
import cn.hutool.core.thread.NamedThreadFactory;
import com.dc.tools.common.IdGenerator;
import com.dc.tools.common.RandomIdGenerator;
import com.dc.tools.common.annotaion.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * delayed timer
 *
 * @author zy
 */
@Slf4j
public class  DelayedTimer extends TimerThread implements Timer,Executor {

    private static final Executor DEFAULT = Executors.newSingleThreadExecutor(new NamedThreadFactory("timer-execute", true));

    /**
     * 延时的时间轮
     */
    private final DelayWheel delayWheel;

    /**
     * 用于执行任务的线程池
     */
    private final Executor executor;

    /**
     * 当前timer的statics
     */
    private final TimerMetrics metrics;

    /**
     * 用于存储目前所有的任务
     */
    private final Map<Long, Task> taskMap = new ConcurrentHashMap<>(1024);

    /**
     * 用于为每个任务生成相应的id
     */
    private final IdGenerator taskIdGenerator = new RandomIdGenerator();

    public DelayedTimer(String serviceName) {
        this(serviceName, DEFAULT);
    }

    public DelayedTimer(String serviceName, Executor executor) {
        super(serviceName);
        this.delayWheel = new DelayWheel();
        this.executor = executor;
        this.metrics = new TimerMetrics();
    }

    @Override
    public void addTask(FixTimeTask fixTimeTask) {
        this.addTask(fixTimeTask, fixTimeTask.delayTime(), TimeUnit.MILLISECONDS);
    }

    @Override
    public void addTask(Task task) {
        this.metrics.record(1);
        this.metrics.finish(1);
        execute(task);
    }

    @Override
    public void execute(@NonNull Runnable command) {
        executor.execute(command);
    }

    @Override
    public void addTask(Task task, long delay, TimeUnit timeUnit) {
        //任务的延迟时间
        long delayTime = timeUnit.toMillis(delay);
        long nowTime = SystemClock.now();
        long taskId = taskIdGenerator.nextId();
        taskMap.put(taskId, task);
        //获取当前的时间
        delayWheel.addTask(task.taskName(), taskId, nowTime + delayTime);
        metrics.record(1);
        wakeup();
    }

    @Override
    public TimerMetrics metrics() {
        return metrics;
    }

    @Override
    protected void onWaitEnd() {

    }

    @Override
    public void run() {
        while (isRunning()) {
            //当前时间
            long nowTime = SystemClock.now();

            long version = getVersion();

            //获取超时的任务
            List<DelayWheel.Task> expireTasks = delayWheel.advance(nowTime);
            metrics.finish(expireTasks.size());
            log.debug("Delay tasks will be executed, delay tasks is: {}", expireTasks);

            //执行过期的任务
            for (DelayWheel.Task expireTask : expireTasks) {
                Task delayedTask = taskMap.remove(expireTask.getTaskId());
                execute(delayedTask);
            }

            //查找时间轮中最早的时间
            long earliestTime = delayWheel.findEarliestTime();

            //如果时间轮中没有任务则阻塞
            if (earliestTime < 0) {
                //直接阻塞等待
                await(version);
                continue;
            }

            //如果时间轮中有任务则阻塞等待
            if (earliestTime > 0) {
                //进行睡眠
                await(version, (int) earliestTime, TimeUnit.MILLISECONDS);
            }
        }

    }

}
