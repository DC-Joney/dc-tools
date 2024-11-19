package com.dc.tools.task;


/**
 * 任务执行器
 *
 * @author zy
 */
public interface TaskProcessor<T extends Task> {

    default void before(Task task, TaskContext taskContext) {

    }

    default void after(Task task, TaskContext taskContext, Exception exception) {

    }


    /**
     * 执行任务
     *
     * @param task 具体需要被执行的任务
     * @return 返回执行任务的状态是成功还是失败，如果是失败状态则会进行重试，如果返回true 即使任务时RetryTask 也不会进行重试
     */
    boolean process(T task, TaskContext taskContext) throws Exception;


    /**
     * 处理器的名称
     */
    default String processorName() {
        return this.getClass().getName();
    }


    default int order() {
        return Integer.MAX_VALUE;
    }

}
