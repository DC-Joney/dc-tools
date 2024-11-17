package com.dc.tools.spring.excel.rule.write;

import com.dc.tools.spring.excel.rule.ExcelWriteRule;
import lombok.RequiredArgsConstructor;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor(staticName = "create")
public class SimpleExcelWriteRule<T> implements ExcelWriteRule<T> {

    private final List<T> dataList;

    private final int sheetNo;

    private final Class<T> headerClass;


    @Override
    public Map<String, Object> getHeaderMap() {
        return Collections.emptyMap();
    }

    @Override
    public List<T> fillDataList() {
        return dataList;
    }

    @Override
    public int sheetNo() {
        return sheetNo;
    }

    @Override
    public Class<T> headerClass() {
        return headerClass;
    }
}
