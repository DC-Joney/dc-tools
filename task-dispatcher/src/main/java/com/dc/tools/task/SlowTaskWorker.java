package com.dc.tools.task;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class SlowTaskWorker extends AbstractTaskWorker<Task> {

    private static final AtomicLong index = new AtomicLong();

    private final int interval;

    public SlowTaskWorker(TaskManager taskManager, int interval) {
        super("slow-worker-" + index.getAndIncrement(), taskManager);
        this.interval = interval;
    }

    public SlowTaskWorker(TaskManager taskManager) {
        super("slow-worker-" + index.getAndIncrement(), taskManager);
        this.interval = 3000;
    }

    @Override
    public TaskType workerType() {
        return TaskType.SLOW;
    }

    @Override
    public TaskWorkerMetrics metrics() {
        return null;
    }

    @Override
    public void run() {
        while (isRunning()) {

            try {

                //获取快照的suze
                int snapshotSize = tasks.size();

                if (snapshotSize == 0) {
                    continue;
                }

                tasks.drain(this::processTask, snapshotSize);
            } finally {
                await(interval, TimeUnit.MILLISECONDS);
            }

        }
    }
}
