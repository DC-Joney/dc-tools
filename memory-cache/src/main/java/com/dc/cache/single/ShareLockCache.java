package com.dc.cache.single;

import lombok.AccessLevel;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.lang.ref.SoftReference;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import java.util.function.Supplier;

/**
 * 基于共享读锁的缓存
 *
 * @author zhangyang
 * @date 2020-09-17
 */
@Deprecated
@SuppressWarnings("Duplicates")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ShareLockCache<T> implements ReferenceCache<T> {

    @Setter
    volatile SoftReference<T> reference;

    final Supplier<T> dataSupplier;

    //共享读锁
    final ShareSync<T> readSync;

    //独占写锁
    final MonopolySync writeSync;

    public ShareLockCache(@NonNull Supplier<T> dataSupplier) {
        this.dataSupplier = dataSupplier;
        this.readSync = new ShareSync<>(this);
        this.writeSync = new MonopolySync();
    }


    public T getCache() {
        readSync.readLock();
        T cacheData = null;

        //保证 JVM垃圾回收不会影响 各个线程的正确操作
        if (reference != null) {
            cacheData = reference.get();
        }

        try {
            if (!hasCache()) {

                //这里要加入互斥的概念，如果在刷新过程中，刚好有线程进入这里，那么数据就会被初始化多次
                writeSync.writeLock();
                if (reference == null || reference.get() == null) {
                    T generate = dataSupplier.get();
                    setReference(new SoftReference<>(generate));
                    cacheData = generate;
                }
            }
        } finally {
            if (writeSync.isWriteLocked()) {
                writeSync.writeUnlock();
            }
            readSync.readUnlock();
        }
        return cacheData;

    }

    /**
     * 刷新缓存
     */

    public void clearCache() {
        try {
            writeSync.writeLock();
            if (hasCache()) {
                reference.clear();
            }
        } finally {
            writeSync.writeUnlock();
        }
    }

    public boolean hasCache() {
        return reference != null && reference.get() != null;
    }


    /**
     * 只能用于第一次初始化的读取，如果数据不存在刷新的情况，则共享读锁可保证第一次初始化，后续读取无问题
     * <p>
     * 如果 存在数据中间状态改变则需要搭配 {@link MonopolySync} 一起使用，在更新完成数据后，将由写锁降级为读锁
     */
    private static class ShareSync<T> extends AbstractQueuedSynchronizer {

        ShareLockCache<T> lockCache;

        /**
         * 线程加锁状态
         */
        private static final int LOCK_STATE = -1;
        /**
         * 线程解锁状态
         */
        private static final int UNLOCK_STATE = 1;


        ShareSync(ShareLockCache<T> lockCache) {
            setState(UNLOCK_STATE);
            this.lockCache = lockCache;
        }

        @Override
        protected int tryAcquireShared(int argState) {
            //如果缓存的值已经不为null，则直接返回
            if (lockCache.hasCache()) {
                return UNLOCK_STATE;
            }
            for (; ; ) {
                int state = getState();

                if (state == LOCK_STATE || getExclusiveOwnerThread() != null)
                    break;

                if (compareAndSetState(UNLOCK_STATE, LOCK_STATE)) {
                    setExclusiveOwnerThread(Thread.currentThread());
                    break;
                }
            }
            if (getExclusiveOwnerThread() == Thread.currentThread()) {
                return UNLOCK_STATE;
            }
            return LOCK_STATE;
        }


        @Override
        protected boolean tryReleaseShared(int arg) {
            setExclusiveOwnerThread(null);
            setState(UNLOCK_STATE);
            return true;
        }


        /**
         * 共享读锁
         */
        private void readLock() {
            acquireShared(1);
        }


        private void readUnlock() {
            releaseShared(1);
        }

    }


    /**
     * 不可重入的独占锁
     */
    private static class MonopolySync extends AbstractQueuedSynchronizer {

        private final int LOCK_STATE = 1;
        private final int UNLOCK_STATE = 0;

        MonopolySync() {
            setState(UNLOCK_STATE);
        }

        @Override
        protected boolean tryAcquire(int arg) {
            if (compareAndSetState(UNLOCK_STATE, LOCK_STATE)) {
                setExclusiveOwnerThread(Thread.currentThread());
                return true;
            }
            return false;

        }

        @Override
        protected boolean tryRelease(int arg) {
            setExclusiveOwnerThread(null);
            setState(UNLOCK_STATE);
            return true;
        }

        public boolean isWriteLocked() {
            return getState() != UNLOCK_STATE;
        }

        /**
         * 独占写锁
         */
        public void writeLock() {
            acquire(1);
        }

        public void writeUnlock() {
            release(1);
        }

    }

}
