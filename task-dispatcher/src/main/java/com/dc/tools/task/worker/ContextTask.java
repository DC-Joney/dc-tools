package com.dc.tools.task.worker;

import com.dc.tools.task.Task;
import com.dc.tools.task.TaskContext;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * internal task wrapper
 *
 * @author zy
 */
@RequiredArgsConstructor
class ContextTask {

    @Getter
    private final Task delegate;

    @Getter
    private final TaskContext taskContext;


}
