package com.dc.tools.task;

public interface TaskWorker <T extends Task>{

    /**
     * 启动worker线程
     */
    void start();

    /**
     * 执行任务
     * @param task 任务
     */
    void execute(T task, TaskContext taskContext);


    /**
     * worker 负责的任务类型
     */
    TaskType workerType();


    /**
     * 当前 worker的 统计信息
     */
    TaskWorkerMetrics metrics();

    /**
     * 停止worker线程
     */
    void shutdown();
}
