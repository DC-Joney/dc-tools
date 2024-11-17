package com.dc.pool.thread.policy;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 线程池拒绝策略
 * @author zhangyang
 */
public class CallerRunOldPolicy implements RejectedExecutionHandler {

    private static final int SPIN_COUNT = 1 << 6;

    @Override
    public void rejectedExecution(Runnable newTask, ThreadPoolExecutor executor) {
        BlockingQueue<Runnable> taskQueue = executor.getQueue();

        //尝试自旋32次，如果还是无法放入则将老任务取出，由当前线程执行
        for (int i = 0; i < SPIN_COUNT; i++) {
            if (taskQueue.offer(newTask)) {
                return;
            }
        }

        Runnable oldTask = executor.getQueue().poll();

        for (;!taskQueue.offer(newTask);)
            break;

        if (oldTask != null)
            oldTask.run();
    }
}
