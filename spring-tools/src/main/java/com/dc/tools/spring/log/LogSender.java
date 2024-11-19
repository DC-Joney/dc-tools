package com.dc.tools.spring.log;

import com.lmax.disruptor.RingBuffer;

public interface LogSender {


    /**
     * 发送日志信息
     *
     * @param logDetail 日志详细信息
     */
    void sendLog(LogDetail logDetail);


}
