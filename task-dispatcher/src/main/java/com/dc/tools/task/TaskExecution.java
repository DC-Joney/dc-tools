package com.dc.tools.task;

import java.util.concurrent.TimeUnit;

/**
 * 任务处理器
 */
public interface TaskExecution {


    /**
     * 添加任务
     *
     * @param task 任务
     */
    void addTask(Task task);

    /**
     * 添加任务
     *
     * @param task        任务
     * @param taskContext 上下文信息
     */
    void addTask(Task task, TaskContext taskContext);


    /**
     * 添加延迟任务
     *
     * @param task      任务名称
     * @param delayTime 延迟时间
     */
    void addDelayedTask(Task task, long delayTime, TimeUnit timeUnit);

    /**
     * 添加延迟任务
     *
     * @param task      任务名称
     * @param delayTime 延迟时间
     */
    void addDelayedTask(Task task, TaskContext taskContext, long delayTime, TimeUnit timeUnit);


    /**
     * 添加延迟任务
     *
     * @param delayedTask 延迟任务
     */
    void addDelayedTask(DelayTask delayedTask);

    /**
     * 添加延迟任务
     *
     * @param delayedTask 延迟任务
     */
    void addDelayedTask(DelayTask delayedTask, TaskContext taskContext);


    /**
     * 删除任务
     *
     * @param taskName 任务名称
     */
    @Deprecated
    void removeTaskName(String taskName);
}
