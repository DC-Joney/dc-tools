package com.dc.cache;

/**
 * Cache value
 *
 * @apiNote Change from redission
 */
public interface CachedValue<K, V> extends ExpirableValue {

    K getKey();

    V getValue();

}

