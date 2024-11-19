package com.dc.tools.spring.log;

import ch.qos.logback.classic.PatternLayout;

public class TraceIdMDCPatternLogbackLayout extends PatternLayout {

    static {
        defaultConverterMap.put("traceId", TraceIdMDCPatternConverter.class.getName());
    }


}
