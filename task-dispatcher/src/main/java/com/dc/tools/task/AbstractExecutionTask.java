package com.dc.tools.task;

import lombok.RequiredArgsConstructor;

/**
 * 抽象实现
 *
 * @author zy
 */
@RequiredArgsConstructor
public abstract class AbstractExecutionTask implements ExecutionTask {

    /**
     * 任务名称
     */
    public final String taskName;

    @Override
    public boolean execute(TaskContext taskContext) {

        Exception exception = null;

        boolean executeState = false;

        try {
            //在执行前回调, 创建新的context
            taskContext = beforeProcess(taskContext);

            //执行具体的任务
            processHandle(taskContext);

        } catch (Exception e) {
            exception = e;
        } finally {
            executeState = afterProcess(taskContext, exception);
        }

        return executeState;
    }


    protected TaskContext beforeProcess(TaskContext taskContext) {
        return taskContext;
    }




    protected abstract void processHandle(TaskContext taskContext) throws Exception;


    /**
     * 任务执行完成后触发回调
     * @param taskContext 任务的上下文
     * @param throwable 异常信息
     */
    protected boolean afterProcess(TaskContext taskContext, Throwable throwable) {
        return throwable == null;
    }


    @Override
    public String taskName() {
        return taskName;
    }
}
