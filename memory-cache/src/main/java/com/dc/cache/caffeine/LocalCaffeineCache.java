package com.dc.cache.caffeine;

import com.dc.cache.CacheRemovedListener;
import com.dc.cache.CachedValue;
import com.dc.tools.common.utils.ConcurrentList;
import com.github.benmanes.caffeine.cache.*;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import io.netty.util.TimerTask;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * 本地缓存实现，通过caffeine实现，caffeine 底层提供了W-TinyLRU的数据结构来存储所有数据，对比传统的LRU实现，W-TinyLFU 提供了高效的算法实现
 *
 * @param <K> cache key
 * @param <V> cache value
 * @see LocalCache
 */
@Slf4j
public class LocalCaffeineCache<K, V> implements com.dc.cache.Cache<K, V> {

    private static final HashedWheelTimer recordsTimer = new HashedWheelTimer();

    /**
     * 创建本地缓存
     */
    private final Cache<K, V> localCache;

    /**
     * 监听Key 过期
     */
    private final ConcurrentList<CacheRemovedListener<K, V>> removedListeners = new ConcurrentList<>();

    @Getter
    private final String cacheName;

    private final Policy<K,V> policy;


    /**
     * @param cacheName       缓存的名称
     * @param initialCapacity 初始化大小
     * @param maximumSize     可以保存最大的缓存数量
     * @param executor        线程池
     * @param scheduler       当有键值对过期时需要通过schduler进行调度 (对于expire类型的键值对时通过TimerWheel时间轮实现的)
     */
    public LocalCaffeineCache(String cacheName, int initialCapacity, long maximumSize, Executor executor, Scheduler scheduler) {
        this.cacheName = cacheName;
        this.localCache = Caffeine.newBuilder()
                .removalListener(new RemoveListener())
                .initialCapacity(initialCapacity)
                .maximumSize(maximumSize)
                .recordStats()
                .ticker(Ticker.systemTicker())
                .scheduler(scheduler)
                .executor(executor)
                .expireAfter(new KeyExpired())
                .softValues()
                .build();

        this.policy = localCache.policy();

        //添加缓存统计指标
        recordsTimer.newTimeout(CacheRecordsTask.of(cacheName, localCache), 1, TimeUnit.MINUTES);

    }

    @AllArgsConstructor(staticName = "of")
    private static class CacheRecordsTask implements TimerTask {

        private String cacheName;

        private Cache<?, ?> cache;

        @Override
        public void run(Timeout timeout) throws Exception {
            log.info("Cache name {} statics detail: {}", cacheName, cache.stats());
            recordsTimer.newTimeout(this, 1, TimeUnit.MINUTES);
        }
    }



    @Override
    public V put(K key, V value, long ttl, TimeUnit ttlUnit) {
        Optional<Policy.VarExpiration<K, V>> expireVariably = localCache.policy().expireVariably();
        V oldValue = localCache.getIfPresent(key);

        if (expireVariably.isPresent()) {
            Policy.VarExpiration<K, V> expiration = expireVariably.get();
            localCache.put(key, value);
            //为新写入的数据添加过期时间
            expiration.setExpiresAfter(key, ttl, ttlUnit);
        }


        return oldValue;
    }

    @Override
    public void refreshTime(K key, long ttl, TimeUnit ttlUnit) {
        Optional<Policy.VarExpiration<K, V>> expirationOptional = localCache.policy().expireVariably();
        if (expirationOptional.isPresent()) {
            Policy.VarExpiration<K, V> expiration = expirationOptional.get();
            if (localCache.getIfPresent(key) != null) {
                expiration.setExpiresAfter(key, ttl, ttlUnit);
            }
        }
    }



    @Override
    public V get(K key, Function<K, V> mappingFunction, long ttl, TimeUnit ttlUnit) {
        //保证原子性
        V value = localCache.get(key, mappingFunction);
        Optional<Policy.VarExpiration<K, V>> expiration = policy.expireVariably();
        expiration.ifPresent(kvVarExpiration -> kvVarExpiration.setExpiresAfter(key, ttl, ttlUnit));
        return value;
    }

    @Override
    public V getAndRefresh(K key, long ttl, TimeUnit ttlUnit) {
        V value = localCache.getIfPresent(key);
        if (value != null) {
            policy.expireVariably().ifPresent(expire-> expire.setExpiresAfter(key, ttl, ttlUnit));
        }

        return value;
    }

    @Override
    public void addRemoveListener(CacheRemovedListener<K, V> removedListener) {
        removedListeners.add(removedListener);
    }

    @Override
    public void removeCache(K cacheKey) {
        localCache.invalidate(cacheKey);
    }

    @Override
    public int size() {
        localCache.cleanUp();
        return (int) localCache.estimatedSize();
    }

    @Override
    public boolean isEmpty() {
        return localCache.asMap().isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return localCache.getIfPresent(key) != null;
    }

    @Override
    public boolean containsValue(Object value) {
        return localCache.asMap().containsValue(value);
    }

    @Override
    public V get(Object key) {
        return localCache.getIfPresent(key);
    }

    @Override
    public V put(K key, V value) {
        //添加永远不过期的键值
        return put(key, value, Long.MAX_VALUE, TimeUnit.NANOSECONDS);
    }

    @Override
    public V remove(Object key) {
        V value = localCache.getIfPresent(key);
        localCache.invalidate(key);
        return value;
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        m.forEach(this::put);
    }

    @Override
    public void clear() {
        localCache.invalidateAll();
    }

    @Override
    public Set<K> keySet() {
        return localCache.asMap().keySet();
    }

    @Override
    public Collection<V> values() {
        return localCache.asMap().values();
    }

    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        throw new UnsupportedOperationException("Cannot support entry set operations");
    }

    class KeyExpired implements Expiry<K, V> {

        @Override
        public long expireAfterCreate(@NonNull K key, @NonNull V value, long currentTime) {
            return Long.MAX_VALUE;
        }

        @Override
        public long expireAfterUpdate(@NonNull K key, @NonNull V value, long currentTime, @NonNegative long currentDuration) {
            return currentDuration;
        }

        @Override
        public long expireAfterRead(@NonNull K key, @NonNull V value, long currentTime, @NonNegative long currentDuration) {
            return currentDuration;
        }
    }


    class RemoveListener implements RemovalListener<K, V> {

        @Override
        public void onRemoval(@Nullable K key, @Nullable V expireValue, @NonNull RemovalCause cause) {

            //如果数据是被人为操作的，那么就会忽略执行removeListener
            // 因为只有在被动的情况下，超过最大限制 或者是被JVM回收 或者是过期时，才应该被通知
            if (cause == RemovalCause.REPLACED || cause == RemovalCause.EXPLICIT) {
                return;
            }

            log.warn("Remove cause: " + cause);

            for (CacheRemovedListener<K, V> removedListener : removedListeners) {

                CachedValue<K, V> cachedValue = new CachedValue<K, V>() {
                    @Override
                    public K getKey() {
                        return key;
                    }

                    @Override
                    public V getValue() {
                        return expireValue;
                    }

                    @Override
                    public boolean isExpired() {
                        return true;
                    }
                };

                removedListener.onRemove(cachedValue);
            }
        }
    }





}
