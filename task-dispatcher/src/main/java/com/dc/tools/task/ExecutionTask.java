package com.dc.tools.task;

/**
 * Runnable Task
 *
 * @author zy
 */
public interface ExecutionTask extends Task {


    boolean execute(TaskContext taskContext) throws Exception;





}
