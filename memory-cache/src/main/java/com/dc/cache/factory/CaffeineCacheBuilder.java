package com.dc.cache.factory;

import com.dc.cache.Cache;
import com.dc.cache.CacheBuilder;
import com.dc.cache.caffeine.LocalCaffeineCache;
import com.github.benmanes.caffeine.cache.Scheduler;
import org.checkerframework.checker.units.qual.K;

import java.util.concurrent.Executor;

/**
 * 用于构建 Caffeine 本地缓存
 *
 * @param <K> Key type
 * @param <V> Value type
 */
public class CaffeineCacheBuilder<K, V> implements Cache.Builder<K, V> {

    /**
     * 缓存的名称
     */
    private String cacheName;

    /**
     * 定时驱逐器
     */
    private Scheduler scheduler;

    /**
     * 缓存可存储的最大的数据量
     */
    private int maxSize;

    /**
     * 缓存初始化的数据量
     */
    private int initialSize;

    /**
     * 用于内部线程执行
     */
    private Executor executor;


    @Override
    public CaffeineCacheBuilder<K, V> cacheName(String cacheName) {
        this.cacheName = cacheName;
        return this;
    }

    @Override
    public CaffeineCacheBuilder<K, V> maxSize(int maxSize) {
        this.maxSize = maxSize;
        return this;
    }

    @Override
    public CaffeineCacheBuilder<K, V> executor(Executor executor) {
        this.executor = executor;
        return this;
    }

    public CaffeineCacheBuilder<K, V> scheduler(Scheduler scheduler) {
        this.scheduler = scheduler;
        return this;
    }


    public CaffeineCacheBuilder<K, V> initialSize(int initialSize) {
        this.initialSize = initialSize;
        return this;
    }


    @Override
    public Cache<K, V> build() {
        return new LocalCaffeineCache<>(cacheName, initialSize, maxSize, executor, scheduler);
    }
}
