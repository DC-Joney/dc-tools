package com.dc.tools.timer;

import java.util.concurrent.ExecutorService;

/**
 * 延迟任务的上层接口
 *
 * @author zy
 */
public interface Timer {

    /**
     * 添加延迟任务
     *
     * @param delayedTask 延迟任务
     */
    void addTask(DelayedTask delayedTask);

    /**
     * 当前 延迟任务池的 统计信息
     */
    TimerMetrics metrics();

    /**
     * 用于创建延迟任务池
     */
    interface Factory {

        /**
         *
         * @param timerName 延时任务池的名称
         */
        Timer createTimer(String timerName);

        /**
         *
         * @param timerName 延时任务池的名称
         * @param executorService 执行延迟任务的线程池
         */
        Timer createTimer(String timerName, ExecutorService executorService);
    }
}
