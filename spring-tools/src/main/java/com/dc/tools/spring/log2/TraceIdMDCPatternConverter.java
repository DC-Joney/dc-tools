package com.dc.tools.spring.log2;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import com.turing.common.trace.opentelemetry.ArmsTraceUtils;

public class TraceIdMDCPatternConverter extends ClassicConverter {

    @Override
    public void start() {
        super.start();
    }

    @Override
    public String convert(ILoggingEvent event) {
        return ArmsTraceUtils.getTraceId().orElse(null);
    }
}
