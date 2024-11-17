package com.dc.tools.spring.excel.event;

import com.dc.tools.spring.excel.rule.ExcelReadRule;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * 读取Sheet页面完成事件
 *
 * @author zhangyang
 * @date 2020-10-13
 */
@Getter
@Setter
@AllArgsConstructor(staticName = "instance")
public class ReadSheetCompleteEvent {


    /**
     * 执行成功或者执行失败
     */
    boolean errorState;

    /**
     * 当前sheet 页名称
     */
    private String sheetName;


    private ExcelReadRule<?> readRule;

}
