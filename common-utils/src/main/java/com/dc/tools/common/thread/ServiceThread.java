package com.dc.tools.common.thread;

import lombok.extern.slf4j.Slf4j;

import java.lang.ref.WeakReference;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;

/**
 * 用于执行部分延迟任务
 * @author zy
 */
@Slf4j
public abstract class ServiceThread implements Runnable {

    private final Sync sync = new Sync();

    private final AtomicBoolean started = new AtomicBoolean();

    private WeakReference<Thread> threadRef;

    private final String serviceName;

    private volatile boolean waiting = false;


    public ServiceThread(String serviceName){
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


    public void await(int timeout, TimeUnit timeUnit)  {
        try {
            waiting = true;
            sync.resetState();
            sync.tryAcquireSharedNanos(1,  TimeUnit.NANOSECONDS.convert(timeout, timeUnit));
        }catch (InterruptedException e) {
            log.error("Interrupted while waiting for synchronization, cause is: ", e);
        }
    }

    public void await(){
        waiting = true;
        sync.tryAcquireShared(1);
    }

    public void wakeup() {
        if (isWaiting()) {
            waiting = false;
            //当等待结束后触发
            onWaitEnd();
            //重置当前的state变量并且唤醒所有的线程
            sync.wakeUp();
        }



    }




    public boolean isWaiting() {
        return waiting && sync.isWaiting();
    }

    protected abstract void onWaitEnd();


    static class Sync extends AbstractQueuedSynchronizer {

        Sync (){
            setState(1);
        }


        void resetState() {
            setState(1);
        }


         void wakeUp(){
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
           return  (getState() == 0) ? 1 : -1;
        }



        @Override
        protected boolean tryReleaseShared(int arg) {

            for (;;) {
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
