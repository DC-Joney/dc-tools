package com.dc.cache;

/**
 * 当缓存中的数据过期或者是被移除时将会被触发
 *
 * @author zhangyang
 */
public interface CacheRemovedListener<K, V> {

    /**
     * The value has be removed
     *
     * @param cacheValue remove cacheValue
     */
    void onRemove(CachedValue<K, V> cacheValue);

}
