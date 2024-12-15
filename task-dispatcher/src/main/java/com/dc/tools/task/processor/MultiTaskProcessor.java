package com.dc.tools.task.processor;

import com.dc.tools.task.Task;
import com.dc.tools.task.TaskProcessor;

import java.util.Collection;

/**
 * 单任务多处理器，用于扩展TaskProcessor实现
 *
 * @param <T>
 */
public interface MultiTaskProcessor<T extends Task> extends TaskProcessor<T> {

    /**
     * 将processor 添加到 尾部
     *
     * @param processor processor 处理器
     */
    MultiTaskProcessor<T> addLast(TaskProcessor<T> processor);

    /**
     * 将 processor 添加到头部
     *
     * @param processor processor
     */
    MultiTaskProcessor<T> addFirst(TaskProcessor<T> processor);


    /**
     * 在指定的processorName 后去添加 processor处理
     *
     * @param processorName prev processor name
     * @param processor     need add processor
     */
    @Deprecated
    MultiTaskProcessor<T> addAfter(String processorName, TaskProcessor<T> processor);

    /**
     * 移除某一个processor
     *
     * @param processor processor
     */
    MultiTaskProcessor<T> remove(TaskProcessor<T> processor);


    /**
     * remove processor by name
     *
     * @param processorName processor name
     */
    MultiTaskProcessor<T> removeProcessor(String processorName);

    /**
     * 获取所有的processor
     */
    Collection<TaskProcessor<T>> getProcessors();

    /**
     * Get first processor from multi
     */
    TaskProcessor<T> getFirst();


}
