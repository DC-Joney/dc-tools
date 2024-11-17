package com.dc.tools.spring.quartz;

/**
 * job 回调方法异常
 *
 * @author zhangyang
 * @date 2020-10-12
 */
public class JobCallbackException extends RuntimeException {

    public JobCallbackException(String message) {
        super(message);
    }

    public JobCallbackException(String message, Throwable cause) {
        super(message, cause);
    }
}
