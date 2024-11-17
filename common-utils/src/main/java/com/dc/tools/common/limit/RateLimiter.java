package com.dc.tools.common.limit;

/**
 * 限流接口
 *
 * @author zhangyang
 */
public interface RateLimiter {

    /**
     * 判断是否已经达到限流阀值
     *
     * @param resource 资源名称
     * @return true: 表示限流
     * false：表示没有限流
     */
    boolean limit(String resource, int maxLimit);

}
