package com.dc.tools.task;

/**
 * 任务路由分发
 *
 * @author zy
 */
public interface TaskRouter {

    /**
     * @param task        任务
     * @param taskType 任务的类型
     * @param taskWorkers 负责task的worker
     */
    TaskWorker<? super Task> getWorker(Task task, TaskType taskType, TaskWorker<? extends Task>[] taskWorkers);
}
