package com.dc.tools.common.thread;

import lombok.extern.slf4j.Slf4j;

import java.lang.ref.WeakReference;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;

/**
 * 用于执行部分延迟任务
 *
 * @author zy
 */
@Slf4j
public abstract class ServiceThread implements Runnable {

    private final Sync sync = new Sync();

    private final AtomicBoolean started = new AtomicBoolean();

    private WeakReference<Thread> threadRef;

    private final String serviceName;

    /**
     * 版本号，用于解决 await 与 wakeup 的并发互斥问题
     */
    private final AtomicLong version = new AtomicLong();

    public ServiceThread(String serviceName) {
        this.serviceName = serviceName;
    }


    /**
     * 启动当前线程
     */
    public void start() {
        if (started.compareAndSet(false, true)) {
            Thread t = new Thread(this, serviceName);
            t.start();
            threadRef = new WeakReference<>(t);
        }
    }

    /**
     * 停止当前线程
     */
    public void stop() {
        if (started.compareAndSet(false, true)) {
            threadRef.clear();
            wakeup();
        }
    }

    /**
     * 如果线程已经结束，那么返回则为null
     */
    public Thread getThread() {
        return threadRef.get();
    }

    public boolean isRunning() {
        return started.get();
    }


    /**
     * @param version  版本号，防止在并发场景下同时出发wakeup以及
     * @param timeout  超时时间
     * @param timeUnit time unit
     */
    public void await(long version, int timeout, TimeUnit timeUnit) {
        try {
            if (this.version.compareAndSet(version, version | 1)) {
                sync.resetState();
                sync.tryAcquireSharedNanos(1, TimeUnit.NANOSECONDS.convert(timeout, timeUnit));
            }
        } catch (InterruptedException e) {
            log.error("Interrupted while waiting for synchronization, cause is: ", e);
        }
    }

    /**
     * 无限制的等待
     *
     * @param version 版本号 用于保证 wakeup 与 await互斥
     */
    public void await(long version) {
        if (this.version.compareAndSet(version, version | 1)) {
            sync.resetState();
            sync.tryAcquireShared(1);
        }
    }

    public void wakeup() {
        //更新版本号
        long newVersion = version.addAndGet(2);

        if (isWaiting()) {
            //当前线程释放cpu，保证加锁完成
            Thread.yield();
            //将低1bit设置为0
            version.set(newVersion >> 1 << 1);
            //当等待结束后触发
            onWaitEnd();
            //重置当前的state变量并且唤醒所有的线程
            sync.wakeUp();
        }
    }


    public boolean isWaiting() {
        return (getVersion() & 1) == 1 || sync.isWaiting();
    }

    /**
     * 获取版本号
     */
    public long getVersion() {
        return version.get();
    }

    protected abstract void onWaitEnd();


    static class Sync extends AbstractQueuedSynchronizer {

        Sync() {
            setState(1);
        }


        void resetState() {
            setState(1);
        }


        void wakeUp() {
            //说明还有线程在阻塞
            if (getState() != 0 || hasQueuedThreads()) {
                //解锁阻塞的线程
                releaseShared(1);
            }
        }

        boolean isWaiting() {
            return getState() != 0;
        }


        @Override
        protected int tryAcquireShared(int arg) {
            return (getState() == 0) ? 1 : -1;
        }


        @Override
        protected boolean tryReleaseShared(int arg) {

            for (; ; ) {
                int state = getState();
                if (state == 0) {
                    return true;
                }

                int newState = state - 1;
                if (compareAndSetState(state, newState)) {
                    return newState == 0;
                }

            }
        }

    }


}
