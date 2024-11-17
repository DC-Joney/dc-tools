package com.dc.tools.spring.excel.rule;

import com.alibaba.excel.context.AnalysisContext;
import com.dc.tools.spring.excel.rule.read.ReadExcelEvent;
import org.springframework.context.ApplicationEventPublisherAware;

import java.util.List;
import java.util.Map;

/**
 * 用于定义 excel文件的单个sheet页的读取规则
 *
 * @param <T>
 * @author zhangyang
 * @see com.dc.tools.spring.excel.sheet.ExcelReadListener
 */
public interface ExcelReadRule<T> extends ApplicationEventPublisherAware {


    /**
     * 一行一行的处理excel的数据
     *
     * @param data    excel解析的行数据
     * @param context excel 行上下文
     */
    void handleExcelData(T data, AnalysisContext context);


    /**
     * 处理excel header数据
     *
     * @param headerMap excel header数据
     */
    void handleHeaderExcelData(Map<Integer, String> headerMap);


    /**
     * 绑定的java类型
     */
    Class<T> excelDataClass();

    /**
     * 判断excel表格是否解析到末尾
     *
     * @param context excel 行上下文
     */
    boolean endCondition(AnalysisContext context);


    /**
     * 同步返回的list类型
     */
    List<T> getSyncDataList();


    /**
     * 返回的对读取事件
     */
    ReadExcelEvent<T> getReadEvent();


    /**
     * 忽略的header行数
     */
    int headerNum();


    /**
     * 是否忽略这个错误
     *
     * @param context excel 行上下文
     */
    boolean ignoreError(AnalysisContext context);

}
