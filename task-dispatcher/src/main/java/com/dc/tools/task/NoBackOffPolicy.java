package com.dc.tools.task;


import com.dc.tools.task.retry.BackoffPolicy;

public class NoBackOffPolicy implements BackoffPolicy {


    @Override
    public long nextTime() {
        return 0;
    }


}
