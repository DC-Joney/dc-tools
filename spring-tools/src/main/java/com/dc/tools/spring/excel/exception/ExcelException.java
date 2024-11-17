package com.dc.tools.spring.excel.exception;

/**
 * 解析Excel出错抛出的异常
 *
 * @author zhangyang
 */
public class ExcelException extends RuntimeException {

    public ExcelException(String message) {
        super(message);
    }

    public ExcelException(String message, Throwable cause) {
        super(message, cause);
    }
}
