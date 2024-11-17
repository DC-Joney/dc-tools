package com.dc.tools.common.exception;

/**
 * 并发异常抽象类
 *
 * @author zhangyang
 * @date 2020-09-07
 */
public abstract class ConcurrentException extends RuntimeException {

    ConcurrentException(Throwable cause) {
        super(cause);
    }

    ConcurrentException(String message) {
        super(message);
    }

    @Override
    public synchronized Throwable fillInStackTrace() {
        return getCause() != null ? getCause().fillInStackTrace() :
                super.fillInStackTrace();
    }

}
