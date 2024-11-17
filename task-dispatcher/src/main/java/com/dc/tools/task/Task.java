package com.dc.tools.task;

/**
 * 执行的任务
 */
public interface Task {


    /**
     * 任务名称
     */
    String taskName();


    /**
     * 返回当前任务的类型, 默认为正常类型
     */
    default TaskType taskType() {
        return TaskType.NORMAL;
    }


    /**
     * @param taskContext 任务上下文信息
     */
    default void before(TaskContext taskContext) {

    }

    /**
     * 在任务执行后调用
     * @param ex 异常信息
     * @param taskContext 任务上下文
     */
    default void after(Exception ex, TaskContext taskContext) {

    }

    @Deprecated
    default void merge(Task otherTask) {

    }

}
