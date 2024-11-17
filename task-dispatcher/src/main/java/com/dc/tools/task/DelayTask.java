package com.dc.tools.task;

/**
 * 延迟任务
 *
 * @author zy
 */
public interface DelayTask extends Task {


    /**
     * 延迟时间
     */
    long delayTime();


    default TaskType taskType() {
        return TaskType.DELAYED;
    }

}
