package com.dc.tools.timer;

import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

/**
 *  delay timer for running delay tasks
 *
 * @author zy
 */
public interface Timer extends Executor{

    /**
     * start the timer
     */
    void start();

    /**
     * 添加延迟任务
     *
     * @param fixTimeTask 延迟任务
     */
    void addTask(FixTimeTask fixTimeTask);

    /**
     * 添加延迟任务
     *
     * @param task 延迟任务
     * @param delay 延迟的时间
     */
    void addTask(Task task, long delay, TimeUnit timeUnit);

    /**
     * 添加延迟任务
     *
     * @param task 延迟任务
     * @param delay 延迟的时间
     */
    void addTask(Task task);




    /**
     * 当前 延迟任务池的 统计信息
     */
    TimerMetrics metrics();

    /**
     * stop timer
     */
    void stop();

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
         * @param executor 执行延迟任务的线程池
         */
        Timer createTimer(String timerName, Executor executor);
    }


}
