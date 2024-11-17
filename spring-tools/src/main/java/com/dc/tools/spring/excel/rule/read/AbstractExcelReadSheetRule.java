package com.dc.tools.spring.excel.rule.read;

import com.dc.tools.spring.excel.rule.ExcelReadSheetRule;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;


/**
 * excel 处理规则
 *
 * @author zhangyang
 */

@Slf4j
public abstract class AbstractExcelReadSheetRule<T> extends AbstractExcelReadRule<T> implements ExcelReadSheetRule<T> {


    public AbstractExcelReadSheetRule(Collection<HeaderKey> headerNames, Class<T> dataClass) {
        super(headerNames, dataClass);
    }
}
