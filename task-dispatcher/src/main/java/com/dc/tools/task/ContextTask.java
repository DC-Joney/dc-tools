package com.dc.tools.task;

import com.dc.common.task.Task;
import com.dc.common.task.TaskContext;
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
