package com.dc.tools.task;

import com.dc.tools.task.retry.BackoffPolicy;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import java.util.concurrent.atomic.AtomicInteger;

@RequiredArgsConstructor
@Getter
@ToString
class RetryContext {

    /**
     * 最大重试次数
     */
    private final int maxRetryTimes;

    /**
     * 最大重试次数
     */
    private final AtomicInteger retryCount = new AtomicInteger();

    /**
     * 最大重试次数
     */
    private final BackoffPolicy backoffPolicy;


}