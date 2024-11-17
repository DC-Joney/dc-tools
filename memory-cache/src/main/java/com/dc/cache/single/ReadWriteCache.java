package com.dc.cache.single;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.springframework.util.Assert;

import javax.annotation.concurrent.ThreadSafe;
import java.lang.ref.SoftReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;

/**
 * 缓存类型信息, {@link ReentrantReadWriteLock} Doug Lea 大师提供的锁降级写法
 *
 * @author zhangyang
 * @date 2020-09-16
 */
@ThreadSafe
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReadWriteCache<T> implements ReferenceCache<T>{

    /**
     * 基于软引用的 缓存
     */
    SoftReference<T> reference;

    final ReadWriteLock readWriteLock;

    final Lock readLock;
    final Lock writeLock;

    /**
     * {@link Supplier} 懒加载构造对象定制接口
     */
    final Supplier<T> supplier;

    public ReadWriteCache(Supplier<T> supplier) {
        this.supplier = supplier;
        this.readWriteLock = new ReentrantReadWriteLock();
        this.readLock = readWriteLock.readLock();
        this.writeLock = readWriteLock.writeLock();
    }



    /**
     * 获取缓存数据
     */

    public T getCache() {

        T cacheData = null;

        readLock.lock();

        //在执行 reference.get() == null 不成立后，JVM如果进行垃圾回收会导致返回数据为null
        if (reference != null && reference.get() != null) {
            cacheData = reference.get();
        }

        if (reference == null || reference.get() == null) {
            // Must release read lock before acquiring write lock
            readLock.unlock();
            writeLock.lock();

            try {

                //初始化缓存数据
                cacheData = supplier.get();
                Assert.notNull(cacheData, "The reference data must not be null");
                this.reference = new SoftReference<>(supplier.get());

                // Downgrade by acquiring read lock before releasing write lock
                readLock.lock();
            } finally {
                writeLock.unlock();
            }
        }

        try {

            T wrapperCache = reference.get();

            if (wrapperCache != null) {
                return wrapperCache;
            }

            return cacheData;
        } finally {
            readLock.unlock();
        }

    }


    /**
     * 刷新缓存数据
     */

    public void clearCache() {
        writeLock.lock();
        try {

            if (this.reference != null) {
                this.reference.clear();
            }

        } finally {
            writeLock.unlock();
        }
    }



}
