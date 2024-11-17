package com.dc.cache.caffeine;

import com.dc.cache.Cache;
import com.github.benmanes.caffeine.cache.Scheduler;
import org.springframework.util.Assert;

import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;

/**
 * 用于构建本地缓存
 *
 * @author zy
 */
public class LocalCacheFactory {


    public static <K, V> LocalCacheBuilder<K, V> builder(String cacheName) {
        return new LocalCacheBuilder<>(cacheName);
    }


    public static class LocalCacheBuilder<K, V> {

        private final String cacheName;

        private Scheduler scheduler;

        private int initialSize;

        private long maximumSize;

        private Executor executor;

        public LocalCacheBuilder(String cacheName) {
            this.cacheName = cacheName;
        }


        public LocalCacheBuilder<K, V> executor(Executor executor) {
            this.executor = executor;
            return this;
        }

        public LocalCacheBuilder<K, V> initialSize(int initialSize) {
            this.initialSize = initialSize;
            return this;
        }

        public LocalCacheBuilder<K, V> maximumSize(long maximumSize) {
            this.maximumSize = maximumSize;
            return this;
        }

        public LocalCacheBuilder<K, V> scheduler(Scheduler scheduler) {
            this.scheduler = scheduler;
            return this;
        }

        public LocalCacheBuilder<K, V> scheduler(ScheduledExecutorService executorService) {
            this.scheduler = Scheduler.forScheduledExecutorService(executorService);
            return this;
        }


        public Cache<K, V> build() {
            if (scheduler == null) {
                scheduler = Scheduler.systemScheduler();
            }


            Assert.notNull(executor, "Executor must not be null");
            Assert.isTrue(initialSize > 0, "initialSize must be > 0");
            Assert.isTrue(maximumSize < Long.MAX_VALUE, "maximumSize must < Integer.MAX_VALUE");
            return new LocalCaffeineCache<>(cacheName, initialSize, maximumSize, executor, scheduler);

        }


    }
}
