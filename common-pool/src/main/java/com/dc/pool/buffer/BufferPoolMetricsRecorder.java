package com.dc.pool.buffer;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.binder.BaseUnits;

import java.util.concurrent.TimeUnit;

/**
 * 内存池统计指标
 *
 * @author zy
 */
public class BufferPoolMetricsRecorder {

    /**
     * base name
     */
    private static final String BASE_NAME = "buffer.pool";

    /**
     * 请求的时间
     */
    private static final String REQUEST_TIME = BASE_NAME + ".request.time";

    /**
     * 请求的次数
     */
    private static final String REQUEST_COUNT = BASE_NAME + ".count";

    /**
     * 等待的线程数
     */
    private static final String WAIT_THREADS = BASE_NAME + ".wait.threads";

    /**
     * 请求失败的次数
     */
    private static final String REQUEST_FAIL_COUNT = BASE_NAME + "request.fail";

    /**
     * 内存池剩余空间
     */
    private static final String FREE_MEMORY = BASE_NAME + ".free";
    /**
     *
     */
    private static final String TOTAL_MEMORY = BASE_NAME + ".total";

    /**
     * pool tag name
     */
    private static final String POOL_TAG_NAME = "name";


    /**
     * 请求的次数
     */
    private final Counter requested;

    /**
     * 线程请求的时间
     */
    private final Timer waitTimer;

    /**
     * 等待的线程数
     */
    private final Gauge waitThreads;

    /**
     * 使用的内存
     */
    private final Gauge freeMemory;

    /**
     * 总共可使用的内存
     */
    private final Counter totalMemory;

    /**
     * 请求失败的次数
     */
    private final Counter requestFail;


    public BufferPoolMetricsRecorder(String poolName, NettyBufferPool bufferPool) {
        this.waitTimer = Timer.builder(REQUEST_TIME)
                .tag(POOL_TAG_NAME, poolName)
                .description("buffer pool request wait timer")
                .publishPercentiles(0.9, 0.8)
                .register(Metrics.globalRegistry);

        this.requested = Counter.builder(REQUEST_COUNT)
                .tag(POOL_TAG_NAME, poolName)
                .description("buffer pool request counter")
                .baseUnit(BaseUnits.OPERATIONS)
                .register(Metrics.globalRegistry);

        this.waitThreads = Gauge.builder(WAIT_THREADS, bufferPool::waitThreads)
                .description("block threads for request buffer")
                .baseUnit(BaseUnits.THREADS)
                .register(Metrics.globalRegistry);

        this.freeMemory = Gauge.builder(FREE_MEMORY, bufferPool::unallocatedMemory)
                .description("buffer pool of free space")
                .tags(POOL_TAG_NAME, poolName)
                .baseUnit(BaseUnits.BYTES)
                .register(Metrics.globalRegistry);

        this.totalMemory = Counter.builder(TOTAL_MEMORY)
                .description("buffer memory total")
                .tags(POOL_TAG_NAME, poolName)
                .baseUnit(BaseUnits.BYTES)
                .register(Metrics.globalRegistry);

        this.requestFail = Counter.builder(REQUEST_FAIL_COUNT)
                .description("buffer pool request fail counter")
                .tags(POOL_TAG_NAME, poolName)
                .baseUnit(BaseUnits.OPERATIONS)
                .register(Metrics.globalRegistry);

        this.totalMemory.increment(bufferPool.totalMemory());
    }

    void requestInc() {
        requested.increment();
    }

    void recordWait(long time) {
        waitTimer.record(time, TimeUnit.MILLISECONDS);
    }

    void failInc() {
        requestFail.increment();
    }


}
