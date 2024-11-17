package com.dc.cache.caffeine;

import com.dc.cache.CacheMetricsTools;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import io.netty.util.TimerTask;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * 用于本地缓存数据
 *
 * @author zhangyang
 */
@Slf4j
public class LocalCache<K, V> extends CaffeineCache {

    private static final HashedWheelTimer timer = new HashedWheelTimer();

    private static final Duration REFRESH_WRITE = Duration.ofMinutes(5);

    private final String cacheName;

    private Function<K, V> loadFunction;

    /**
     * 用于统计缓存的内存情况
     */
    private Timeout timeout;

    private static <K, V> Cache<K, V> createLocalCache(String cacheName, Executor executor, Function<K, V> function, Duration refreshAfterWrite) {
        LocalCacheLoader<K, V> cacheLoader = new LocalCacheLoader<>(function, cacheName);
        return Caffeine.newBuilder()
                .recordStats()
                .executor(executor)
                .maximumSize(1000)
                //当JVM内存不足时，可以保证被SoftReference 引用的缓存可以被回收
                //TODO 是否改为 weakValues
                .softValues()
                //默认刷新时间为5分钟
                .refreshAfterWrite(refreshAfterWrite)
                .build(cacheLoader);
    }

    public LocalCache(String cacheName, Executor executor, Function<K, V> loadFunction) {
        this(cacheName, executor, loadFunction, REFRESH_WRITE);
    }

    public LocalCache(String cacheName, Function<K, V> loadFunction) {
        this(cacheName, CacheMetricsTools.CACHE_POOL, loadFunction, REFRESH_WRITE);
    }

    public LocalCache(String cacheName, Function<K, V> loadFunction, Duration refreshAfterWrite) {
        this(cacheName, CacheMetricsTools.CACHE_POOL, loadFunction, refreshAfterWrite);
    }

    @SuppressWarnings("unchecked")
    public LocalCache(String cacheName, Executor executor, Function<K, V> loadFunction, Duration refreshAfterWrite) {
        super(cacheName, createLocalCache(cacheName, executor, (Function<Object, Object>) loadFunction, refreshAfterWrite));
        this.cacheName = cacheName;
        this.loadFunction = loadFunction;
        init();
    }

    private void init() {
        StatsTimeout statsTimeout = new StatsTimeout(getNativeCache(), this.cacheName);
        this.timeout = timer.newTimeout(statsTimeout, 1, TimeUnit.MINUTES);
    }

    public void destroy() {
        this.timeout.cancel();
    }

    @Override
    public <T> T get(@org.springframework.lang.NonNull Object key, @org.springframework.lang.NonNull Callable<T> valueLoader) {
        throw new UnsupportedOperationException("Cannot support getValue from valueLoader");
    }

    @Override
    @SuppressWarnings("unchecked")
    protected Object lookup(@org.springframework.lang.NonNull Object key) {
        if (getNativeCache() instanceof LoadingCache) {
            LoadingCache<Object, Object> loadingCache = (LoadingCache<Object, Object>) getNativeCache();
            return loadingCache.get(key);
        }

        return getNativeCache().get(key, (Function<Object, Object>) loadFunction);
    }


    @AllArgsConstructor
    private static class LocalCacheLoader<K, V> implements CacheLoader<K, V> {

        private final Function<K, V> loadFunction;

        private final String cacheName;

        @Override
        public @Nullable V load(@NonNull K key) throws Exception {
            try {
                log.info("Load value from {}, the key is {}", cacheName, key);
                return loadFunction.apply(key);
            } catch (Exception e) {
                log.info("load value from function error, cacheName is: {}", this.cacheName);
            }

            return null;
        }

        @Override
        public @Nullable V reload(@NonNull K key, @NonNull V oldValue) throws Exception {
            V reloadValue = load(key);

            //当reloadValue为空时，返回旧的值
            if (reloadValue == null) {
                reloadValue = oldValue;
                log.error("Reload value error, fallback oldValue to cache, cacheName is {}, reload key is {}", cacheName, key);
            }

            return reloadValue;
        }
    }


    /**
     * 用于统计当前缓存使用情况
     */
    @AllArgsConstructor
    private class StatsTimeout implements TimerTask {

        private final Cache<?, ?> localCache;

        private final String cacheName;

        @Override
        public void run(Timeout timeout) throws Exception {
            log.info("Cache name {} statics detail: {}", cacheName, localCache.stats());
            timer.newTimeout(this, 1, TimeUnit.MINUTES);
        }
    }


}
