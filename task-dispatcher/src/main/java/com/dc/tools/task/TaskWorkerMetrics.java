package com.dc.tools.task;

/**
 * task worker metrics
 *
 * @author zy
 */
public interface TaskWorkerMetrics {


    /**
     * 增长率
     */
    double increment();


    /**
     * 任务数量
     */
    long taskCount();


    /**
     * 任务执行的平均时间
     */
    double avgTime();

    /**
     * 已经添加的所有任务数量
     */
    long allTaskCount();

    /**
     * 已经处理的任务数量
     */
    long handleTaskCount();


    /**
     * worker 处理的比率
     */
    double handleRatio();
}
