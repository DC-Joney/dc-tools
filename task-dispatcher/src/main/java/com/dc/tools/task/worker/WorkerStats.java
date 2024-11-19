package com.dc.tools.task.worker;

import com.codahale.metrics.*;
import com.dc.tools.common.utils.SystemClock;
import com.dc.tools.common.window.StaticsWindow;
import com.dc.tools.task.TaskWorkerMetrics;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * worker 线程的任务统计
 *
 * @author zy
 */
public class WorkerStats implements TaskWorkerMetrics {


    /**
     * 当前活跃的任务数量
     */
    private final Gauge<Long> activeTasks;

    /**
     * 添加的所有任务数量
     */
    private final Counter received;

    /**
     * 已经处理的任务数量
     */
    private final Counter handled;


    private final StaticsWindow receivedTaskWindow = new StaticsWindow(1000, 8);


    private final StaticsWindow handleTaskWindow = new StaticsWindow(1000, 8);

    private volatile long lastHandleTime;

    private final Meter recievedMeter;

    private Timer timer;

    /**
     * @param registry metrics registry
     * @param tasks    活跃的任务数量
     */
    public WorkerStats(MetricRegistry registry, String workerName, Supplier<Long> tasks) {
        this.timer = registry.timer("handle timeout");
        this.timer = registry.register(MetricRegistry.name(workerName, "execute time"), new Timer());
        this.recievedMeter = registry.register(MetricRegistry.name(workerName, "received meter"), new Meter());
        this.received = registry.register(MetricRegistry.name(workerName, "add tasks"), new Counter());
        this.handled = registry.register(MetricRegistry.name(workerName, "handle tasks"), new Counter());
        this.activeTasks = registry.register("", new Gauge<Long>() {
            @Override
            public Long getValue() {
                return tasks.get();
            }
        });

        this.lastHandleTime = SystemClock.now();
    }

    /**
     * 获取最近一分钟内的增长速率
     */
    @Override
    public double increment() {
        return recievedMeter.getOneMinuteRate();
    }

    @Override
    public long taskCount() {
        return activeTasks.getValue();
    }

    @Override
    public long allTaskCount() {
        return received.getCount();
    }

    @Override
    public long handleTaskCount() {
        return handled.getCount();
    }

    void incReceivedTask() {
        receivedTaskWindow.addCount(SystemClock.now(), 1);
        received.inc();
        recievedMeter.mark();
    }

     void incHandledTask() {
        handleTaskWindow.addCount(SystemClock.now(), 1);
        handled.inc();
    }


    /**
     * 记录执行的时间
     *
     * @param executeTime 每次执行任务所需要的时间
     */
     void recordTime(long executeTime) {
        timer.update(executeTime, TimeUnit.MILLISECONDS);
    }

    /**
     * 任务执行的平均时间
     */
    public double avgTime() {
        return timer.getSnapshot().getMean();
    }


    /**
     * 更新最后处理的时间
     */
     void updateLastTime() {
        this.lastHandleTime = SystemClock.now();
    }

    /**
     * 处理任务的速率
     */
    public double handleRatio() {
        //时间窗口内添加任务的数量
        int addTasks = receivedTaskWindow.sumCount();
        //时间窗口内处理任务的数量
        int handleTasks = handleTaskWindow.sumCount();
        //获取处理的速率
        return handleTasks / addTasks == 0 ? 1 : addTasks;
    }

    /**
     * 当前线程空闲的时间
     */
    public long freeTime() {
        return SystemClock.now() - lastHandleTime;
    }
}
