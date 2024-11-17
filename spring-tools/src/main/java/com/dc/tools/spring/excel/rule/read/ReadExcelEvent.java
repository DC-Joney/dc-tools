package com.dc.tools.spring.excel.rule.read;


import java.util.List;

/**
 * 读取excel数据事件
 *
 * @author zhangyang
 */
public class ReadExcelEvent<T> extends BaseExcelDataEvent<List<T>> {
    public ReadExcelEvent(List<T> dataList) {
        super(dataList);
    }
}
