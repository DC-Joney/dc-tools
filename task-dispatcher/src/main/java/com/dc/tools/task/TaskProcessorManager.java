package com.dc.tools.task;

import com.dc.tools.task.processor.MultiTaskProcessor;

import java.util.Collection;

/**
 * 用于添加任务处理器，处理相应的任务数据
 *
 * @author zy
 */
public interface TaskProcessorManager {


    /**
     * 用于处理默认的任务，既没有处理器也不是 ExecutionTask 类型的任务
     *
     * @param processor 任务处理器
     */
    void addDefaultProcessor(TaskProcessor<? extends Task> processor);


    /**
     * 添加任务以及任务的执行器, 如果存在则会抛出异常，建议使用{@link TaskProcessorManager#newProcessor(String)}的方式进行添加
     *
     * @param processor 任务处理器
     * @param taskName 任务名称
     */
    void addProcessor(String taskName, TaskProcessor<? extends Task> processor);


    /**
     * 删除任务名称下的所有 任务处理器
     *
     * @param taskName 任务名称
     */
    void removeProcessor(String taskName);


    /**
     * 获取task 对应的处理器
     * @param taskName 任务名称
     * @param create 如果不存在该task的处理器是否需要进行创建
     */
    <T extends Task> MultiTaskProcessor<T> getProcessor(String taskName);


    /**
     * 获取task 对应的处理器
     * @param taskName 任务名称
     * @param create 如果不存在该task的处理器是否需要进行创建
     */
    <T extends Task> MultiTaskProcessor<T> newProcessor(String taskName);


    /**
     * 获取task 对应的处理器
     * @param taskName 任务名称
     */
    TaskProcessor<? extends Task> getSingleProcessor(String taskName);


    /**
     * 获取所有的任务处理器
     */
    Collection<TaskProcessor<? extends Task>> getAllProcessors();


    /**
     * 获取默认的任务处理器
     */
    MultiTaskProcessor<Task> getDefaultProcessor();


}
