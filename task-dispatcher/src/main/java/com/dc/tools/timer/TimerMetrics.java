package com.dc.tools.timer;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.dc.tools.common.utils.SystemClock;
import com.dc.tools.common.window.StaticsWindow;

import java.util.concurrent.atomic.LongAdder;

public class TimerMetrics {

    /**
     * 所有的任务数量
     */
    private final LongAdder allTasks = new LongAdder();

    /**
     * 正在计算的任务数量
     */
    private final LongAdder activeTasks = new LongAdder();

    /**
     * 滑动窗口用于记录某个时间窗口内的任务
     * TODO: 滑动窗口存在问题不支持负数，需要修改
     */
    private final StaticsWindow statistics = new StaticsWindow(1000, 32);

    private final MetricRegistry registry = new MetricRegistry();

    public TimerMetrics() {
        Meter meter = registry.meter("test.1");
    }


    public void record(int taskCount) {
        allTasks.add(taskCount);
        activeTasks.add(taskCount);
        //向当前窗口添加数据
        statistics.addCount(SystemClock.now(), taskCount);
    }

    /**
     * 任务结束时调用
     *
     * @param taskCount 任务结束的数量，在每次扫描到期时会调用
     */
    public void finish(int taskCount) {
        activeTasks.add(-taskCount);
        statistics.addCount(SystemClock.now(), -1);
    }

    public LongAdder getAllTasks() {
        return allTasks;
    }

    public LongAdder getActiveTasks() {
        return activeTasks;
    }

    /**
     * 时间轮中任务增长的速率
     */
    public double incrementRatio() {


        return -1;
    }

}
