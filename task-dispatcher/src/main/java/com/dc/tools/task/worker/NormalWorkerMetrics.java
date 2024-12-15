package com.dc.tools.task.worker;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.dc.tools.common.window.StaticsWindow;
import com.dc.tools.task.TaskWorkerMetrics;

import java.util.Queue;

public class NormalWorkerMetrics implements TaskWorkerMetrics {

    private final StaticsWindow addTaskWindow = new StaticsWindow(1000,8);


    private final Counter counter;

    private final Gauge<Long> taskCounts;


    public NormalWorkerMetrics(MetricRegistry metricRegistry, Queue<?> queue) {
        this.counter = metricRegistry.counter("");
        this.taskCounts = metricRegistry.gauge("task.count", () -> new Gauge<Long>() {
            @Override
            public Long getValue() {
                return (long) queue.size();
            }
        });
    }


    public void incTasks(){
        counter.inc();
    }


    @Override
    public double increment() {
        return 0;
    }

    @Override
    public long taskCount() {
        return taskCounts.getValue();
    }

    @Override
    public double avgTime() {
        return 0;
    }

    @Override
    public long allTaskCount() {
        return counter.getCount();
    }

    @Override
    public long handleTaskCount() {
        return 0;
    }

    @Override
    public double handleRatio() {
        return 0;
    }

    @Override
    public long freeTime() {
        return 0;
    }
}
