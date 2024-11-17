package com.dc.tools.spring.excel;

import com.dc.tools.spring.excel.rule.ExcelReadRule;
import org.springframework.context.ApplicationEventPublisherAware;

/**
 * excel 多Sheet页读取管理
 *
 * @author zhangyang
 */
public interface ExcelReadSingleManagement extends ApplicationEventPublisherAware {

    /**
     * 读取excel数据
     *
     * @return 多sheet页面的excel管理
     */
    ExcelReadSingleManagement readExcel(ExcelReadRule<?> readRule);


}
