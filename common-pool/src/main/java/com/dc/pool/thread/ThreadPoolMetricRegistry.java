package com.dc.pool.thread;

import com.codahale.metrics.*;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

/**
 * @author jiachun.fjc
 *
 * @apiNote Fork from <a href="https://github.com/sofastack/sofa-jraft">Soft-Jraft</a>
 */
@Slf4j
public class ThreadPoolMetricRegistry {

    private static final MetricRegistry metricRegistry = new MetricRegistry();
    private static final ThreadLocal<Timer.Context> timerThreadLocal = new ThreadLocal<>();

    private static final ScheduledReporter reporter;

    static {
        reporter = Slf4jReporter.forRegistry(metricRegistry())
                .withLoggingLevel(Slf4jReporter.LoggingLevel.INFO)
                .convertDurationsTo(TimeUnit.SECONDS)
                .convertRatesTo(TimeUnit.SECONDS)
                .outputTo(log)
                .shutdownExecutorOnStop(true)
                .build();

        reporter.start(1, TimeUnit.MINUTES);
    }

    /**
     * Return the global registry of metric instances.
     */
    public static MetricRegistry metricRegistry() {
        return metricRegistry;
    }

    public static ThreadLocal<Timer.Context> timerThreadLocal() {
        return timerThreadLocal;
    }
}