package com.dc.cache;

import com.dc.pool.thread.NamedThreadFactory;
import com.dc.pool.thread.ThreadPoolUtil;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 用于获取监控信息
 *
 * @author zy
 */
public class CacheMetricsTools {

    /**
     * 定制绘本 数据同步处理线程池
     */
    public static final ThreadPoolExecutor CACHE_POOL;

    static {
        CACHE_POOL = ThreadPoolUtil.newBuilder()
                .poolName("CACHE_REFRESH")
                .coreThreads(10)
                .enableMetric(true)
                .maximumThreads(10)
                .keepAliveSeconds(3600L)
                //ring-buffer
                .workQueue(new ArrayBlockingQueue<>(2000))
                .threadFactory(new NamedThreadFactory("cache-"))
                .rejectedHandler(new ThreadPoolExecutor.CallerRunsPolicy())
                .build();
    }


}
