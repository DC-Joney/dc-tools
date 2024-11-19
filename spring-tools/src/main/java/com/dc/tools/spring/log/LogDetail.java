package com.dc.tools.spring.log;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class LogDetail {

    /**
     * 日志id
     */
    private long id;

    /**
     * 全局id，表示链路id
     */
    private String globalId;

    /**
     * 顺序问题
     */
    private String order;

    /**
     * class name
     */
    private String className;

    /**
     * 方法名称
     */
    private String methodName;

    /**
     * 数据来源
     */
    private SourceType sourceType;

    /**
     * 来源类型
     */
    private MethodType methodType;

    /**
     * 线程id
     */
    private long threadId;

    /**
     * 应用名称
     */
    private String appName;

    /**
     * 请求来源的ip地址
     */
    private String ip;

    /**
     * 方法开始的的时间
     */
    private long time;

    /**
     * 方法执行的时间
     */
    private long timeUsed;

    /**
     * 输入的参数
     */
    private List<Object> arguments;

    /**
     * 输出的参数
     */
    private Object result;

    /**
     * 异常信息
     */
    private Throwable exception;


}
