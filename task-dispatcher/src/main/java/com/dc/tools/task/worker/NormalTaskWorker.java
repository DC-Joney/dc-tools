package com.dc.tools.task.worker;

import com.dc.tools.task.*;

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

            //版本号
            long version = getVersion();

            if (contextTask == null) {
                await(version);
                continue;
            }

            processTask(contextTask);
        }
    }


}
