package com.dc.tools.task;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * delay task wrapper
 *
 * @author zy
 */
@RequiredArgsConstructor
public class DelayedTaskWrapper implements DelayTask {

    @Getter
    private final Task delegate;

    private final long delayTime;

    @Override
    public long delayTime() {
        return delayTime;
    }

    @Override
    public String taskName() {
        return delegate.taskName();
    }



}