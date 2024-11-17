package com.dc.tools.spring.excel.rule.read;

import com.alibaba.excel.context.AnalysisContext;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;


/**
 * excel 处理规则
 *
 * @author zhangyang
 */

@Slf4j
public class SimpleExcelReadRule<T> extends AbstractExcelReadRule<T> {

    private final int headerNum;


    @SuppressWarnings("all")
    public SimpleExcelReadRule(Class<T> dataClass, int headerNum) {
        super(Collections.emptyList(), dataClass);
        this.headerNum = headerNum;
    }

    @Override
    public boolean endCondition(AnalysisContext context) {
        return false;
    }

    @Override
    public int headerNum() {
        return headerNum;
    }


    @Override
    protected T doAfterRead(T data) {
        return data;
    }

}
