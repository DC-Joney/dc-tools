package com.dc.tools.spring.excel.exception;


import lombok.extern.slf4j.Slf4j;

import java.text.MessageFormat;

/**
 * excel解析异常
 *
 * @author zhangyang
 */
@Slf4j
public class ExcelParseException extends RuntimeException {


    public ExcelParseException(String message) {
        super(message);
    }

    /**
     * resolve args into message
     */
    public ExcelParseException(String message, Object... args) {
        super(MessageFormat.format(message, args));
    }

    public ExcelParseException(Throwable cause) {
        super(cause);
    }

    public ExcelParseException(String message, Throwable cause) {
        super(message, cause);
    }

}
