package com.dc.tools.task;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class NormalTaskWorker extends AbstractTaskWorker<Task> implements TaskWorker<Task> {

    private static final AtomicLong index = new AtomicLong();

    public NormalTaskWorker(TaskManager taskManager) {
        super("fast-worker-" + index.getAndIncrement(), taskManager);
    }


    @Override
    public void execute(Task task, TaskContext taskContext) {
        super.execute(task, taskContext);
        wakeup();
    }

    @Override
    public TaskType workerType() {
        return TaskType.NORMAL;
    }

    @Override
    public TaskWorkerMetrics metrics() {
        return null;
    }

    @Override
    public void run() {
        while (isRunning()) {
            ContextTask contextTask = tasks.poll();

            if (contextTask == null) {
                await(5, TimeUnit.SECONDS);
                continue;
            }

            processTask(contextTask);
        }
    }


}
