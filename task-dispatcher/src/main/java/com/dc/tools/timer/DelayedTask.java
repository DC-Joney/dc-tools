package com.dc.tools.timer;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public abstract class DelayedTask implements Runnable{

    @Getter
    private final String taskName;

    @Override
    public void run() {

        Exception exception = null;

        try {
            beforeExecute();
            runTask();
        }
        catch (Exception e) {
            exception = e;
        }finally {
            afterExecute(exception);
        }
    }

    /**
     * 任务执行前的回调
     */
    protected void beforeExecute(){

    }

    /**
     * 任务执行后的回调
     * @param e 任务执行出现异常时的回调
     */
    protected void afterExecute(Exception e){

    }


    /**
     * 执行具体的任务
     */
    protected abstract void runTask();



    /**
     * 任务的延迟时间
     */
    protected abstract long delayTime();
}
