package com.dc.tools.task.retry;


import com.dc.tools.task.Task;
import com.dc.tools.task.retry.BackoffPolicy;

public interface RetryTask extends Task {

    /**
     * 最大重试次数
     */
    int maxRetries();

    /**
     * 重试任务的延迟策略
     */
    BackoffPolicy backoffPolicy();


    /**
     * 是否需要忽略该异常
     * @param exception 异常信息
     */
    default boolean ignoreException(Exception exception) {
        return false;
    }
}
