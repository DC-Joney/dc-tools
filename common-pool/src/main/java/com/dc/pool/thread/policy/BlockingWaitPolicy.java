package com.dc.pool.thread.policy;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 阻塞类型的拒绝策略
 *
 * @author zhangyang
 */
public class BlockingWaitPolicy implements RejectedExecutionHandler {

    private static final int RETRY_MAX_TIMES = 3;
    private static final RejectedExecutionHandler fallBackHandler = new ThreadPoolExecutor.CallerRunsPolicy();

    @Override
    public void rejectedExecution(Runnable task, ThreadPoolExecutor executor) {
        //如果由于其他原因导致当前线程被中断而丢失数据，则直接忽略当前中断

        int retryTimes = 0;
        boolean retryFail = false;

        boolean interrupt = false;
        for (; ; ) {
            try {
                //对应于 put操作，实现思路都是一样的，但是put（有可能会导致）数据放入之后出错
                if (executor.getQueue().offer(task, 100, TimeUnit.MICROSECONDS))
                    break;

                if (retryTimes >= RETRY_MAX_TIMES) {
                    retryFail = true;
                    break;
                }

                retryTimes++;
            } catch (InterruptedException e) {
                interrupt = true;
            }
        }
        if (interrupt)
            Thread.currentThread().interrupt();

        //如果最后还是尝试失败的话，则执行对应的拒绝策略
        if (retryFail)
            fallBackHandler.rejectedExecution(task, executor);
    }
}
