package com.dc.cache.single;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import javax.annotation.concurrent.ThreadSafe;
import java.lang.ref.SoftReference;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/**
 * 基于自旋的缓存
 *
 * @author zhangyang
 * @date 2020-09-16
 */
@ThreadSafe
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SpinCache<T> {

    /**
     * 缓存的数据
     */
    @Setter
    volatile SoftReference<T> reference;

    final Supplier<T> supplier;

    /**
     * 执行成功
     */
    final int SUCCESS = 1;

    /**
     * 初始状态或者 执行失败状态
     */
    final int INVALID = -1;

    /**
     * 获取数据状态
     */
    final int GETTING = -2;


    final int UNKNOWN = -3;

    /**
     * 默认状态为失效状态
     */
    @Getter
    @Setter
    volatile int state = INVALID;

    private static final AtomicIntegerFieldUpdater<SpinCache> S
            = AtomicIntegerFieldUpdater.newUpdater(SpinCache.class, "state");

    private static final int MAP_LRU_SIZE = 1000;

    /**
     * 基于 LRU算法的 Map
     */
    final Map<Long, Integer> lruLoopMap = new LinkedHashMap<Long, Integer>(32) {

        @Override
        protected boolean removeEldestEntry(Map.Entry eldest) {
            return size() > MAP_LRU_SIZE;
        }
    };

    /**
     * 线程批次状态
     */
    AtomicLong loopState = new AtomicLong(0);


    public SpinCache(Supplier<T> supplier) {
        this.supplier = supplier;
    }

    /**
     * 读取缓存数据
     */
    public T read() {
        T cache = null;
        //如果 reference 不等于空，则先尝试拿到 cache的值
        if (reference != null) {
            cache = reference.get();
        }
        if (reference == null || reference.get() == null) {
            //当数据状态不为 GETTING 也不为 INVALID
            if (getState() > 0) {
                setState(INVALID);
            }
            long loopStateQ = loopState.get();
            if (S.compareAndSet(this, INVALID, GETTING)) {
                int state = INVALID;
                try {
                    cache = Objects.requireNonNull(supplier.get(), "The reference supplier produce must not be null");
                    setReference(new SoftReference<>(cache));
                    state = SUCCESS;

                } finally {
                    lruLoopMap.putIfAbsent(loopStateQ, state);
                    setState(state);
                    //保障释放掉自旋锁以后再对版本进行增加，避免增加版本后，后续无线程再拥有该自旋锁
                    loopState.incrementAndGet();
                }
            }
            for (; ; ) {
                int state = lruLoopMap.getOrDefault(loopStateQ, UNKNOWN);
                //如果是成功状态则直接返回
                if (state == SUCCESS)
                    break;

                //如果是失效状态则直接抛出异常
                if (state == INVALID) {
                    throw new IllegalStateException("获取数据失败");
                }
            }
        }
        T referenceCache = reference.get();

        //如果在初始化完成后，由于系统资源紧张被回收了，则将初始化后的值返回
        if (referenceCache == null) {
            S.compareAndSet(this, SUCCESS, INVALID);
        }
        return cache;
    }

    /**
     * 刷新缓存
     */
    public void refresh() {

        if (getState() == GETTING)
            return;

        //如果状态为INVALID或者是 GETTING，直接忽略就好
        if (S.compareAndSet(this, SUCCESS, INVALID)) {
            this.reference.clear();
        }

    }
}
