package com.dc.tools.task;

/**
 * 用于处理、管理所有任务
 *
 * @author zy
 */
public interface TaskManager extends TaskExecution, TaskProcessorManager {

    /**
     * 根据不同的任务类型获取不同的worker 处理线程
     */
    TaskWorker<?>[] taskWorkers(TaskType taskType);


    /**
     * 根据任务计算出对应的worker线程
     * <p>最终会通过{@code taskType}来计算具体的worker线程</p>
     *
     * @param task     任务
     * @param taskType 任务类型
     */
    TaskWorker<? super Task> route(Task task, TaskType taskType);


    /**
     * 根据任务计算出对应的worker线程
     *
     * @param task 任务
     */
    TaskWorker<? super Task> route(Task task);


    /**
     * 获取 生成的下一个taskId, 内部方法不建议调用，除非是在自定义的TaskContext的场景下使用
     */
    TaskIdGenerator idGenerator();
}
