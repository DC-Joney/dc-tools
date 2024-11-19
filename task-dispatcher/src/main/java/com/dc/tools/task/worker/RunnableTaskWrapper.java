package com.dc.tools.task.worker;

import com.dc.tools.common.SequenceUtil;
import com.dc.tools.task.ExecutionTask;
import com.dc.tools.task.TaskContext;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class RunnableTaskWrapper implements ExecutionTask {

    private final Runnable runnable;

    @Override
    public boolean execute(TaskContext taskContext) throws Exception {
        runnable.run();
        return true;
    }

    @Override
    public String taskName() {
        return SequenceUtil.nextIdString();
    }


}
