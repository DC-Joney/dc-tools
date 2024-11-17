package com.dc.tools.task.retry;


import java.util.concurrent.ThreadLocalRandom;

/**
 * Implementation of {@link BackoffPolicy} that pauses for a random period of
 * time before continuing.
 * <p>
 * {@link #setMinBackOffPeriod(long)} is thread-safe and it is safe to call
 * {@link #setMaxBackOffPeriod(long)} during execution from multiple threads, however
 * this may cause a single retry operation to have pauses of different
 * intervals.
 *
 * @author Fork from spring retry
 */
public class UniformRandomBackOffPolicy implements BackoffPolicy {

    private static final long DEFAULT_BACK_OFF_MIN_PERIOD = 500L;

    /**
     * Default max back off period - 1500ms.
     */
    private static final long DEFAULT_BACK_OFF_MAX_PERIOD = 1500L;

    /**
     * Default max back off period - 1500ms.
     */
    private volatile long minBackOffPeriod = DEFAULT_BACK_OFF_MIN_PERIOD;

    private volatile long maxBackOffPeriod = DEFAULT_BACK_OFF_MAX_PERIOD;

    public void setMinBackOffPeriod(long backOffPeriod) {
        this.minBackOffPeriod = (backOffPeriod > 0 ? backOffPeriod : 1);
    }

    /**
     * The minimum backoff period in milliseconds.
     *
     * @return the backoff period
     */
    public long getMinBackOffPeriod() {
        return minBackOffPeriod;
    }

    /**
     * Set the maximum back off period in milliseconds. Cannot be &lt; 1. Default value
     * is 1500ms.
     *
     * @param backOffPeriod the back off period
     */
    public void setMaxBackOffPeriod(long backOffPeriod) {
        this.maxBackOffPeriod = (backOffPeriod > 0 ? backOffPeriod : 1);
    }

    /**
     * The maximum backoff period in milliseconds.
     *
     * @return the backoff period
     */
    public long getMaxBackOffPeriod() {
        return maxBackOffPeriod;
    }


    @Override
    public long nextTime() {
        return maxBackOffPeriod == minBackOffPeriod ? 0 :
                ThreadLocalRandom.current().nextInt((int) (maxBackOffPeriod - minBackOffPeriod));
    }


}
