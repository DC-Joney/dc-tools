package com.dc.cache;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * Map interface supporting expiration time
 *
 * @author zhangyang
 */
public interface Cache<K, V> extends Map<K, V> {

    /**
     * Put value to local cache
     *
     * @param key     cacheKey
     * @param value   cache value
     * @param ttl     cache max ttl
     * @param ttlUnit ttl unit
     * @return Return cache map prev value
     */
    V put(K key, V value, long ttl, TimeUnit ttlUnit);

    /**
     * 添加value到缓存，并且刷新对应的缓存时间
     *
     * @param key     cacheKey
     * @param ttl     cache max ttl
     * @param ttlUnit ttl unit
     */
    void refreshTime(K key, long ttl, TimeUnit ttlUnit);


    /**
     * 获取对应的数据，如果数据不存在则会通过{@param mappingFunction}进行添加
     * 并且会设置相应的缓存时间，如果数据存在则会直接刷新缓存时间
     * @param key 缓存key
     * @param mappingFunction 当value不存在时创建对应的value
     * @param ttl 缓存的过期时间
     * @param ttlUnit 时间单位
     */
    V get(K key, Function<K, V> mappingFunction, long ttl, TimeUnit ttlUnit);

    /**
     * 获取对应的数据，并且刷新过期时间
     * @param key 缓存key
     * @param ttl 缓存的过期时间
     * @param ttlUnit 时间单位
     */
    V getAndRefresh(K key, long ttl, TimeUnit ttlUnit);



    default V put(K key, V value) {
        return put(key, value, Long.MAX_VALUE, TimeUnit.MILLISECONDS);
    }


    void addRemoveListener(CacheRemovedListener<K, V> removedListener);


    void removeCache(K cacheKey);

}
