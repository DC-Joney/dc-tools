package com.dc.tools.spring.quartz;

/**
 * 创建任务异常
 *
 * @author zhangyang
 * @date 2020-10-12
 */
public class JobCreateException extends RuntimeException {

    public JobCreateException(String message) {
        super(message);
    }

    public JobCreateException(String message, Throwable cause) {
        super(message, cause);
    }
}
