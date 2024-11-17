package com.dc.tools.spring.exception;

/**
 * 返回的状态码
 * @author zhangyang
 */
public interface ResultStatus {

    /**
     * 状态码
     */
    int getCode();

    /**
     * 提示的message
     */
    String getMessage();
}
