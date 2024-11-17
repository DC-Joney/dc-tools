package com.dc.tools.task.retry;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 弹性延迟策略
 *
 * @author zy
 */
public class JemitterBackoffPolicy implements BackoffPolicy {

    /**
     * The default 'initialInterval' value - 100 millisecs. Coupled with the default
     * 'multiplier' value this gives a useful initial spread of pauses for 1-5 retries.
     */
    public static final Duration DEFAULT_MIN_INTERVAL = Duration.ofMillis(100);

    /**
     * The default maximum backoff time (30 seconds).
     */
    public static final Duration DEFAULT_MAX_INTERVAL = Duration.ofMillis(30000);

    /**
     * The default 'multiplier' value - value 2 (100% increase per backoff).
     */
    public static final double DEFAULT_MULTIPLIER = 2;


    /**
     * The value to increment the exp seed with for each retry attempt.
     */
    private final double jitterFactor;

    private final Duration minBackoff;
    private final Duration maxBackoff;

    /**
     * 迭代的次数
     */
    private final AtomicInteger iteration = new AtomicInteger();

    public JemitterBackoffPolicy() {
        this(DEFAULT_MULTIPLIER);
    }

    public JemitterBackoffPolicy(double jitterFactor) {
        this(jitterFactor, DEFAULT_MIN_INTERVAL, DEFAULT_MAX_INTERVAL);
    }

    public JemitterBackoffPolicy(double jitterFactor, Duration minBackoff, Duration maxBackoff) {
        this.jitterFactor = jitterFactor;
        this.maxBackoff = maxBackoff;
        this.minBackoff = (minBackoff.isZero() || minBackoff.isNegative()) ? DEFAULT_MIN_INTERVAL : minBackoff;
    }


    @Override
    public long nextTime() {
        Duration nextBackoff;
        try {
            nextBackoff = minBackoff.multipliedBy((long) Math.pow(2, iteration.getAndIncrement()));
            if (nextBackoff.compareTo(maxBackoff) > 0) {
                nextBackoff = maxBackoff;
            }
        } catch (ArithmeticException overflow) {
            nextBackoff = maxBackoff;
        }

        //short-circuit delay == 0 case
        if (nextBackoff.isZero()) {
            nextBackoff = minBackoff;
        }

        ThreadLocalRandom random = ThreadLocalRandom.current();

        long jitterOffset;
        try {
            jitterOffset = nextBackoff.multipliedBy((long) (100 * jitterFactor))
                    .dividedBy(100)
                    .toMillis();
        } catch (ArithmeticException ae) {
            jitterOffset = Math.round(Long.MAX_VALUE * jitterFactor);
        }

        long lowBound = Math.max(minBackoff.minus(nextBackoff)
                .toMillis(), -jitterOffset);

        long highBound = Math.min(maxBackoff.minus(nextBackoff)
                .toMillis(), jitterOffset);

        long jitter;
        if (highBound == lowBound) {
            if (highBound == 0) jitter = 0;
            else jitter = random.nextLong(highBound);
        } else {
            jitter = random.nextLong(lowBound, highBound);
        }

        return nextBackoff.plusMillis(jitter).toMillis();
    }


}
