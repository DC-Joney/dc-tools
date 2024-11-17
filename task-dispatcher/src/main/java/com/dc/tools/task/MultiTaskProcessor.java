package com.dc.tools.task;

import java.util.Collection;

public interface MultiTaskProcessor<T extends Task> extends TaskProcessor<T>{


    MultiTaskProcessor<T> addLast(TaskProcessor<T> processor);

    MultiTaskProcessor<T> addFirst(TaskProcessor<T> processor);

    MultiTaskProcessor<T> addAfter(String processorName, TaskProcessor<T> processor);


    MultiTaskProcessor<T> remove(TaskProcessor<T> processor);


    MultiTaskProcessor<T> removeProcessor(String processorName);

    Collection<TaskProcessor<T>> getProcessors();

    TaskProcessor<T> getFirst();



}
