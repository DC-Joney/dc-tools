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
public class SimpleExcelReadSheetRule<T> extends AbstractExcelReadSheetRule<T> {

    private final int headerNum;

    private final int sheetIndex;

    @SuppressWarnings("all")
    public SimpleExcelReadSheetRule(Class<T> dataClass, int headerNum, int sheetIndex) {
        super(Collections.emptyList(), dataClass);
        this.headerNum = headerNum;
        this.sheetIndex = sheetIndex;
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
    public int sheetIndex() {
        return sheetIndex;
    }

    @Override
    public String sheetName() {
        return null;
    }

    @Override
    protected T doAfterRead(T data) {
        return data;
    }

}
