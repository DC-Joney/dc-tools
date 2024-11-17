package com.dc.tools.spring.excel;

import com.dc.tools.spring.excel.rule.ExcelReadSheetRule;
import org.springframework.context.ApplicationEventPublisherAware;

import java.util.List;

/**
 * excel 多Sheet页读取管理
 *
 * @author zhangyang
 */
public interface ExcelReadManagement extends ApplicationEventPublisherAware {

    /**
     * 读取excel数据
     *
     * @return 多sheet页面的excel管理
     */
    ExcelReadManagement readExcel(List<ExcelReadSheetRule<?>> readRules);


}
