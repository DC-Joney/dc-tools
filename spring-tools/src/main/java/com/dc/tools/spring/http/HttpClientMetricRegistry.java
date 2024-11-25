package com.dc.tools.spring.http;

import com.codahale.metrics.*;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Slf4j
public class HttpClientMetricRegistry {

    private static final MetricRegistry metricRegistry = new MetricRegistry();

    private static final ThreadLocal<Timer.Context> timerThreadLocal = new ThreadLocal<>();

    private static final ScheduledReporter reporter;

    static {
        reporter = Slf4jReporter.forRegistry(metricRegistry)
                .outputTo(log)
                .withLoggingLevel(Slf4jReporter.LoggingLevel.ERROR)
                .convertDurationsTo(TimeUnit.SECONDS)
                .convertRatesTo(TimeUnit.SECONDS)
                .shutdownExecutorOnStop(true)
                .build();

        reporter.start(5, TimeUnit.MINUTES);
    }

    public static MetricRegistry getMetricRegistry() {
        return metricRegistry;
    }

    public static synchronized Counter getCounter(String metricName) {
        Counter counter;
        if (!metricRegistry.getCounters().containsKey(metricName)) {
            counter = new Counter();
            metricRegistry.register(metricName, counter);
            return counter;
        }

        return metricRegistry.getCounters().get(metricName);
    }

    public static synchronized Meter getMeter(String metricName) {
        Meter meter;
        if (!metricRegistry.getMeters().containsKey(metricName)) {
            meter = new Meter();
            metricRegistry.meter(metricName, ()-> meter);
            return meter;
        }

        return metricRegistry.getMeters().get(metricName);
    }

    public static void newRatioGauge(String name, RatioGauge.Ratio ratio) {
        metricRegistry.gauge(name, () -> new RatioGauge() {
            @Override
            protected Ratio getRatio() {
                return ratio;
            }
        });
    }

    public static <T> void newCachedGauge(String name, T instance) {
        //缓存5s内的数据
        metricRegistry.gauge(name, () -> new CachedGauge<T>(5, TimeUnit.SECONDS) {
            @Override
            protected T loadValue() {
                return instance;
            }
        });
    }

    public static <T> void newGauge(String name, T instance) {
        //缓存5s内的数据
        Gauge<T> gauge = () -> instance;
        metricRegistry.register(name, gauge);
    }

    public static <T> boolean containsMetric(String name) {
        return metricRegistry.getGauges().containsKey(name);
    }

    public static synchronized <T> boolean addIfAbsent(String name, Gauge<T> gauge) {
        if (!containsMetric(name)) {
            metricRegistry.gauge(name, ()-> gauge);
            return true;
        }

        return false;
    }

    public static synchronized <T> boolean addRatioGaugeIfAbsent(String name, Supplier<RatioGauge.Ratio> ratioSupplier) {
        if (!containsMetric(name)) {
            metricRegistry.gauge(name, ()-> new RatioGauge(){
                @Override
                protected Ratio getRatio() {
                    return ratioSupplier.get();
                }
            });
            return true;
        }

        return false;
    }


    public static ThreadLocal<Timer.Context> getTimerThreadLocal() {
        return timerThreadLocal;
    }

    public static synchronized Histogram getHistogram(String metricName) {
        Histogram histogram = metricRegistry.getHistograms().get(metricName);
        if (histogram == null) {
            histogram = new Histogram(new ExponentiallyDecayingReservoir());
            metricRegistry.register(metricName, histogram);
        }

        return histogram;
    }

    public static synchronized Timer getTimer(String metricName) {
        Timer timer = metricRegistry.getTimers().get(metricName);
        if (timer == null) {
            timer = new Timer(new ExponentiallyDecayingReservoir());
            metricRegistry.register(metricName, timer);
        }

        return timer;
    }
}
