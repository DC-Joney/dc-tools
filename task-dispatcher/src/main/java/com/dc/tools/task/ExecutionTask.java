package com.dc.tools.task;

/**
 * Execution Task
 *
 * @author zy
 */
public interface ExecutionTask extends Task {


    /**
     * 用户可执行的任务
     * @param taskContext 任务的上下文
     */
    boolean execute(TaskContext taskContext) throws Exception;

}
