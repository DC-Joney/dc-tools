package com.dc.tools.task;

/**
 * 任务类型
 */
public class TaskType {

    /**
     * 正常的任务
     */
    public static final int NORMAL_TYPE = 0;

    public static final TaskType NORMAL = new TaskType(NORMAL_TYPE);

    /**
     * 慢任务
     */
    public static final int SLOW_TYPE = 1;

    public static final TaskType SLOW = new TaskType(SLOW_TYPE);


    /**
     * 延迟任务
     */
    public static final int DELAYED_TYPE = 1 << 1;

    public static final TaskType DELAYED = new TaskType(DELAYED_TYPE);


    private final int type;


    TaskType(int type) {
        this.type = type;
    }


    public boolean isNormal() {
        return (type & SLOW_TYPE) == 0;
    }

    public boolean isSlow() {
        return (type & SLOW_TYPE) != 0;
    }

    public boolean isDelay() {
        return (type & DELAYED_TYPE) != 0;
    }

    public int interestType() {
        return type;
    }

    public TaskType interestType(int type) {
        return new TaskType(type);
    }

}
