package com.dc.tools.task.retry;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 重试的上下文信息
 */
@RequiredArgsConstructor
@Getter
@ToString
public class RetryContext {

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