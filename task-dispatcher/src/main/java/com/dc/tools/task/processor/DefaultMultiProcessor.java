package com.dc.tools.task.processor;

import cn.hutool.core.builder.EqualsBuilder;
import cn.hutool.core.builder.HashCodeBuilder;
import com.dc.tools.task.Task;
import com.dc.tools.task.TaskContext;
import com.dc.tools.task.TaskProcessor;
import lombok.Getter;

import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.stream.Collectors;

/**
 * 默认是支持幂等的实现
 *
 * @author zy
 */
public class DefaultMultiProcessor implements MultiTaskProcessor<Task> {

    private final ConcurrentLinkedDeque<NamedTaskProcessor> processors = new ConcurrentLinkedDeque<>();


    @Override
    public MultiTaskProcessor<Task> addLast(TaskProcessor<Task> processor) {
        processors.addLast(new NamedTaskProcessor(processor));
        return this;
    }

    @Override
    public MultiTaskProcessor<Task> addFirst(TaskProcessor<Task> processor) {
        processors.addFirst(new NamedTaskProcessor(processor));
        return this;
    }

    @Override
    public MultiTaskProcessor<Task> addAfter(String processorName, TaskProcessor<Task> processor) {
        throw new UnsupportedOperationException("Cannot support addAfter method");
    }



    @Override
    public boolean process(Task task, TaskContext taskContext) {
        boolean success = true;
        for (NamedTaskProcessor namedTaskProcessor : processors) {
            TaskProcessor<Task> processor = namedTaskProcessor.processor;

            try {
                Long taskId = taskContext.taskId();
                Boolean executeState = taskContext.get(processor.processorName() + taskId, Boolean.class);
                //如果是执行成功的processor则不再进行处理
                if (executeState != null && executeState) {
                    continue;
                }

                //处理的状态
                boolean process = processor.process(task, taskContext);
                if (success) {
                    success = process;
                }

                if (process) {
                    taskContext.put(processor.processorName() + taskId, true);
                }
            }catch (Exception e) {
                success = false;
            }
        }

        return success;
    }

    @Override
    public MultiTaskProcessor<Task> remove(TaskProcessor<Task> processor) {
        processors.removeIf(pro-> pro.processorName.equals(processor.processorName()));
        return this;
    }

    @Override
    public MultiTaskProcessor<Task> removeProcessor(String processorName) {
        processors.removeIf(pro-> pro.processorName.equals(processorName));
        return this;
    }

    @Override
    public Collection<TaskProcessor<Task>> getProcessors() {
        return processors.stream()
                .map(NamedTaskProcessor::getProcessor)
                .collect(Collectors.toList());
    }

    @Override
    public TaskProcessor<Task> getFirst() {

        NamedTaskProcessor namedProcessor = processors.peekFirst();
        if (namedProcessor != null) {
            return namedProcessor.processor;
        }

        return null;
    }

    @Override
    public void before(Task task, TaskContext taskContext) {

    }

    static class NamedTaskProcessor implements Comparable<NamedTaskProcessor>{

        private final String processorName;

        private final int order;

        @Getter
        private final TaskProcessor<Task> processor;


        public NamedTaskProcessor( TaskProcessor<Task> processor) {
            this.processorName = processor.processorName();
            this.order = processor.order();
            this.processor = processor;
        }


        @Override
        public int compareTo(NamedTaskProcessor o) {
            return Integer.compare(this.order, o.order);
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder().append(processorName).hashCode();
        }


        @Override
        public boolean equals(Object obj) {
            return new EqualsBuilder().append(processorName, processorName).isEquals();
        }
    }

}
