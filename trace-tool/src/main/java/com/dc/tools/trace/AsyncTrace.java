package com.dc.tools.trace;

/**
 * 用于包装异步执行任务，返回链路id
 *
 * @author zhangyang
 */
public interface AsyncTrace {

    /**
     * 用于获取当前链路的id
     * @return 返回当前链路的id
     */
    String requestId();
}
