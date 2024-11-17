package com.dc.tools.spring.excel;

import com.alibaba.excel.converters.Converter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class ExcelUtilsConfig {


    @Bean
    public ExcelUtils excelUtils(List<Converter<?>> converters) {
        //注册转换器
        ExcelUtils.addConverters(converters);
        return ExcelUtils.getSharedInstance();
    }
}
