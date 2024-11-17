package com.dc.tools.task.retry;

import java.time.Duration;

/**
 * 固定延迟策略
 *
 * @author zy
 */
public class FixDelayBackoffPolicy implements BackoffPolicy {

    private final Duration duration;

    public FixDelayBackoffPolicy(Duration duration) {
        this.duration = duration;
    }

    @Override
    public long nextTime() {
        return duration.toMillis();
    }



}
