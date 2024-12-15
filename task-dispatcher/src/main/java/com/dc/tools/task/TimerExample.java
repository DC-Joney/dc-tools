package com.dc.tools.task;

import com.dc.tools.common.annotaion.JustForTest;

import java.util.concurrent.TimeUnit;

public class TimerExample {


    @JustForTest
    public void taskAdd() {

        TaskManager taskManager = TaskDispatchCenter.getInstance();

        TaskContext taskContext = new TaskContext();
        taskContext.addCallback(new TaskCallback() {
            @Override
            public void onCallback(Throwable throwable) {

            }
        });
        taskManager.addDelayedTask(new ExecutionTask() {

            @Override
            public void before(TaskContext taskContext) {
                ExecutionTask.super.before(taskContext);
            }

            @Override
            public void after(Exception ex, TaskContext taskContext) {
                ExecutionTask.super.after(ex, taskContext);
            }

            @Override
            public boolean execute(TaskContext taskContext) throws Exception {
                return true;
            }

            @Override
            public String taskName() {
                return "222222";
            }
        }, taskContext, 3, TimeUnit.SECONDS);


    }
}
