package com.dc.cache.single;

/**
 * 基于软引用的高速缓存
 *
 * @author zhangyang
 * @date 2020-10-13
 */
public interface ReferenceCache<T> {

    /**
     * 获取引用缓存数据
     */
    T getCache();

    /**
     * 清空缓存
     */
    void clearCache();

}
