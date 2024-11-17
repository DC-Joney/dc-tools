package com.dc.tools.spring.excel.rule;

/**
 * 用于定义 excel文件的单个sheet页的读取规则
 *
 * @param <T>
 * @author zhangyang
 * @see com.dc.tools.spring.excel.sheet.ExcelReadListener
 */
public interface ExcelReadSheetRule<T> extends ExcelReadRule<T> {



    /**
     * 绑定的sheet index
     */
    int sheetIndex();

    /**
     * 绑定的sheet name, 与sheetName选择一个实现即可
     */
    String sheetName();



}
