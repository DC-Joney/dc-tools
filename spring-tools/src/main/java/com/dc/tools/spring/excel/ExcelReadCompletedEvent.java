package com.dc.tools.spring.excel;


import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * excel 读取完成事件
 *
 * @author zhangyang
 */
@RequiredArgsConstructor
public class ExcelReadCompletedEvent {

    /**
     * 同步 vs  半同步
     */
    @Getter
    private final boolean asyncState;

}
