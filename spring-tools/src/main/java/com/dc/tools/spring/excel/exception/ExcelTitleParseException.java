package com.dc.tools.spring.excel.exception;


/**
 * excel 表头匹配错误
 *
 * @author zhangyang
 */
public class ExcelTitleParseException extends RuntimeException {
    public ExcelTitleParseException(String message) {
        super(message);
    }

}
