package com.dc.tools.task.worker;

import com.codahale.metrics.MetricRegistry;
import com.dc.tools.task.Task;
import com.dc.tools.task.TaskManager;
import com.dc.tools.task.TaskType;
import com.dc.tools.task.TaskWorkerMetrics;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class SlowTaskWorker extends AbstractTaskWorker<Task> {

    private static final AtomicLong index = new AtomicLong();

    private final int interval;

    public SlowTaskWorker(TaskManager taskManager, int interval) {
        super("slow-worker-" + index.getAndIncrement(), taskManager);
        this.interval = interval;
    }

    public SlowTaskWorker(TaskManager taskManager, MetricRegistry metricRegistry) {
        super("slow-worker-" + index.getAndIncrement(), taskManager, metricRegistry);
        this.interval = 3000;
    }

    @Override
    public TaskType workerType() {
        return TaskType.SLOW;
    }



    @Override
    public void run() {
        while (isRunning()) {
            long version = getVersion();

            try {
                //获取快照的suze
                int snapshotSize = tasks.size();

                if (snapshotSize == 0) {
                    continue;
                }

                tasks.drain(this::processTask, snapshotSize);
            } finally {
                await(version, interval, TimeUnit.MILLISECONDS);
            }

        }
    }

}
