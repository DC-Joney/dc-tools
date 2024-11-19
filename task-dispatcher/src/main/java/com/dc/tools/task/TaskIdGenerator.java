package com.dc.tools.task;

/**
 * 用于生成taskId
 *
 * @author zy
 */
public interface TaskIdGenerator {

    /**
     * 生成taskId
     */
    long nextId();

}
