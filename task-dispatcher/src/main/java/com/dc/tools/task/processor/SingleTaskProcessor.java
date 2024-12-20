package com.dc.tools.task.processor;

import com.dc.tools.task.Task;
import com.dc.tools.task.TaskContext;
import com.dc.tools.task.TaskProcessor;
import lombok.RequiredArgsConstructor;

import java.util.Collection;

/**
 * 支持单个任务的 {@link MultiTaskProcessor},
 * 有一些特殊的情况下我们我们的单个task只需要被单个processor处理
 */
@RequiredArgsConstructor
public class SingleTaskProcessor implements MultiTaskProcessor<Task> {

    private final TaskProcessor<Task> taskProcessor;

    @Override
    public MultiTaskProcessor<Task> addLast(TaskProcessor<Task> processor) {
        throw new UnsupportedOperationException("Unsupported task processor for method addLast");
    }

    @Override
    public MultiTaskProcessor<Task> addFirst(TaskProcessor<Task> processor) {
        throw new UnsupportedOperationException("Unsupported task processor for method addLast");

    }

    @Override
    public MultiTaskProcessor<Task> addAfter(String processorName, TaskProcessor<Task> processor) {
        throw new UnsupportedOperationException("Unsupported task processor for method addLast");

    }

    @Override
    public MultiTaskProcessor<Task> remove(TaskProcessor<Task> processor) {
        throw new UnsupportedOperationException("Unsupported task processor for method addLast");

    }

    @Override
    public MultiTaskProcessor<Task> removeProcessor(String processorName) {
        throw new UnsupportedOperationException("Unsupported task processor for method addLast");

    }

    @Override
    public Collection<TaskProcessor<Task>> getProcessors() {
        throw new UnsupportedOperationException("Unsupported task processor for method addLast");

    }

    @Override
    public TaskProcessor<Task> getFirst() {
        throw new UnsupportedOperationException("Unsupported task processor for method addLast");

    }

    @Override
    public boolean process(Task task, TaskContext taskContext) throws Exception {
        return taskProcessor.process(task, taskContext);
    }

    @Override
    public String processorName() {
        return taskProcessor.processorName();
    }
}
