package com.dc.tools.spring.http;

/**
 * 生命周期接口
 *
 * @author zhangyang
 */
public interface LiftCycle {

    /**
     * start method
     *
     * @throws Exception 启动时抛出的异常
     */
    void start() throws Exception;

    /**
     * stop method
     */
    void stop();

}
