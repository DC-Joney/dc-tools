package com.dc.tools.task;

/**
 * 任务在生命周期结束后会进行回调，默认只回调一次，不受任务重试以及内部任务的轮转影响
 *
 * @author zy
 */
public interface TaskCallback {

    /**
     * 在任务执行前回调
     * @param throwable 如果存在异常时回调异常
     */
    void onCallback(Throwable throwable);


}
