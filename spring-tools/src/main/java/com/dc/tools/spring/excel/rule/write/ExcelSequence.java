package com.dc.tools.spring.excel.rule.write;

import com.alibaba.excel.annotation.ExcelProperty;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public abstract class ExcelSequence {

    @ExcelProperty("序号")
    @JsonIgnore
    private String seq;
}
