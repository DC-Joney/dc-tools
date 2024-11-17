package com.dc.tools.common.utils;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;

/**
 * 只能用于单次的 阻塞等待器
 *
 * @author zhangyang
 * @date 2020-09-07
 * @see java.util.concurrent.CountDownLatch
 */
public class SignalLatch {

    /**
     * 同步等待器
     */
    private Sync sync = new Sync();


    /**
     * 创建一个基于单次的阻塞等待器
     */
    public static SignalLatch create() {
        return new SignalLatch();
    }


    private static class Sync extends AbstractQueuedSynchronizer {

        public Sync() {
            setState(1);
        }

        @Override
        protected boolean tryReleaseShared(int arg) {

            //将 state 设置为0 则表示共享锁解锁
            setState(0);
            return true;
        }


        @Override
        protected int tryAcquireShared(int arg) {

            //返回1 则表示当前锁已经损坏，且线程不会阻塞，返回 -1 表示线程将阻塞在CHL中
            return -getState();
        }
    }


    /**
     * 尝试阻塞等待拥有锁
     * <p>
     * 忽略线程中断
     */
    public void await() {
        sync.acquireShared(1);
    }

    /**
     * 尝试阻塞等待拥有锁
     * <p>
     * 忽略线程中断
     */
    public void awaitInterruptibly() throws InterruptedException {
        sync.acquireSharedInterruptibly(1);
    }

    /**
     * 尝试阻塞等待拥有锁
     * <p>
     * 忽略线程中断
     */
    public void awaitInterruptibly(long time, TimeUnit unit) {

        long waitTime, acquireTime;

        //需要等待的时间
        waitTime = acquireTime = unit.toNanos(time);

        //开始时间
        long startTime = System.nanoTime();

        //结束时间
        long endTime = startTime + acquireTime;

        do {

            try {
                sync.tryAcquireSharedNanos(1, waitTime);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                waitTime = Math.max(acquireTime + startTime - System.nanoTime(), 0);
            }

        } while (endTime > System.nanoTime());
    }


    /**
     * 返回在CHL队列中的线程等待数量
     */
    public int waitCount() {
        return sync.getQueueLength();
    }


    /**
     * 唤醒在当前锁上的所有线程
     */
    public void signalAll() {
        sync.releaseShared(1);
    }

    @Override
    public String toString() {
        return super.toString();
    }

}
