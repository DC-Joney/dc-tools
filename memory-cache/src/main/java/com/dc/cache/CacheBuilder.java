package com.dc.cache;

public interface CacheBuilder<K, V> {


    CacheBuilder<K, V> cacheName(String cacheName);


    CacheBuilder<K, V> maxSize(int maxSize);


    Cache<K, V> build();

}
