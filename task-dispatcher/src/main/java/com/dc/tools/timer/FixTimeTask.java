package com.dc.tools.timer;

/**
 * 固定时间的延迟任务
 *
 * @author zy
 */
public abstract class FixTimeTask extends AbstractTask{

    /**
     * @param taskName task name
     */
    public FixTimeTask(String taskName) {
        super(taskName);
    }

    /**
     * 任务的延迟时间
     */
    protected abstract long delayTime();

}
