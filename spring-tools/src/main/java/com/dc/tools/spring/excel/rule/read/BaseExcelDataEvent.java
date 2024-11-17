package com.dc.tools.spring.excel.rule.read;

import org.springframework.context.ApplicationEvent;

/**
 * 读取Excel 完成事件
 *
 * @author zhangyang
 */
public class BaseExcelDataEvent<T> extends ApplicationEvent {

    public BaseExcelDataEvent(T source) {
        super(source);
    }
}
