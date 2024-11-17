package com.dc.tools.common.exception;

/**
 * 多异常包装器
 *
 * @author zhangyang
 * @date 2020-09-07
 */
public class CompositeException extends ConcurrentException {

    public CompositeException() {
        super("Multiple exceptions");
    }
}
