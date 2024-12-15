package com.dc.tools.task.retry;


/**
 * No back off
 *
 * @author zy
 * @see BackoffPolicy
 * @see FixDelayBackoffPolicy
 * @see UniformRandomBackOffPolicy
 * @see JemitterBackoffPolicy
 */
public class NoBackOffPolicy implements BackoffPolicy {


    @Override
    public long nextTime() {
        return 0;
    }


}
