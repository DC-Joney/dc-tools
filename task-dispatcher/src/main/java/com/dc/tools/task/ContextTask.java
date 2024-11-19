package com.dc.tools.task;

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
