package com.dc.tools.spring.log;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import com.dc.tools.trace.TraceUtils;

public class TraceIdMDCPatternConverter extends ClassicConverter {

    @Override
    public void start() {
        super.start();
    }

    @Override
    public String convert(ILoggingEvent event) {
        return TraceUtils.getTraceId().orElse(null);
    }
}
