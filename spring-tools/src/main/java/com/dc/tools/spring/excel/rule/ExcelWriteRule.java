package com.dc.tools.spring.excel.rule;

import com.alibaba.excel.write.handler.WriteHandler;

import java.util.List;
import java.util.Map;

/**
 * excel 写入接口
 *
 * @param <T>
 * @author zhangyang
 */
public interface ExcelWriteRule<T> extends WriteHandler {


    /**
     * 填充的头部 以及其他类型
     */
    Map<String, Object> getHeaderMap();


    /**
     * 填充的数据
     */
    List<T> fillDataList();


    /**
     * 对应的excel sheet页数
     */
    int sheetNo();


    /**
     * ExcelManagement 依赖的 java bean class
     */
    Class<T> headerClass();

}
