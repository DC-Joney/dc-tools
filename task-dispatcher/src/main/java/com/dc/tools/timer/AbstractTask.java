package com.dc.tools.timer;

/**
 * 任务抽象实现
 *
 * @author zy
 */
public abstract class AbstractTask implements Task {

    /**
     * 任务名称
     */
    private final String taskName;

    /**
     * 任务是否退出
     */
    private volatile boolean cancel;


    public AbstractTask(String taskName) {
        this.taskName = taskName;
    }

    @Override
    public String taskName() {
        return taskName;
    }


    @Override
    public void run() {
        //如果任务已经退出则不再执行
        if (cancel)
            return;

        Exception exception = null;

        try {
            beforeExecute();
            runTask();
        } catch (Exception e) {
            exception = e;
        } finally {
            afterExecute(exception);
        }
    }

    /**
     * 任务执行前的回调
     */
    protected void beforeExecute() {

    }

    /**
     * 任务执行后的回调
     *
     * @param e 任务执行出现异常时的回调
     */
    protected void afterExecute(Exception e) {

    }


    /**
     * 执行具体的任务
     */
    protected abstract void runTask();


    @Override
    public void cancel() {
        this.cancel = true;
    }
}
