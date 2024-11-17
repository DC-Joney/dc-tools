package com.dc.tools.task.retry;

/**
 * 延迟策略
 *
 * @author zy
 */
public interface BackoffPolicy {


    /**
     * 下次执行任务的时间
     *
     * @param retryCount 当前重试的次数
     */
    long nextTime();


}
