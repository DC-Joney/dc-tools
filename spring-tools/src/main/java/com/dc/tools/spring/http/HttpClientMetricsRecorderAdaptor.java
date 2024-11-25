package com.dc.tools.spring.http;

import reactor.netty.http.client.HttpClientMetricsRecorder;

import java.net.SocketAddress;
import java.time.Duration;

/**
 * Adaptor for {@linkplain HttpClientMetricsRecorder}
 *
 * @author zhangyang
 * @see reactor.netty.channel.ChannelMetricsRecorder
 * @see reactor.netty.http.server.HttpServerMetricsRecorder
 */
public class HttpClientMetricsRecorderAdaptor implements HttpClientMetricsRecorder {

    @Override
    public void recordDataReceivedTime(SocketAddress remoteAddress, String uri, String method, String status, Duration time) {

    }

    @Override
    public void recordDataSentTime(SocketAddress remoteAddress, String uri, String method, Duration time) {

    }

    @Override
    public void recordResponseTime(SocketAddress remoteAddress, String uri, String method, String status, Duration time) {

    }

    @Override
    public void recordDataReceived(SocketAddress remoteAddress, String uri, long bytes) {

    }

    @Override
    public void recordDataSent(SocketAddress remoteAddress, String uri, long bytes) {

    }

    @Override
    public void incrementErrorsCount(SocketAddress remoteAddress, String uri) {

    }

    @Override
    public void recordDataReceived(SocketAddress remoteAddress, long bytes) {

    }

    @Override
    public void recordDataSent(SocketAddress remoteAddress, long bytes) {

    }

    @Override
    public void incrementErrorsCount(SocketAddress remoteAddress) {

    }

    @Override
    public void recordTlsHandshakeTime(SocketAddress remoteAddress, Duration time, String status) {

    }

    @Override
    public void recordConnectTime(SocketAddress remoteAddress, Duration time, String status) {

    }

    @Override
    public void recordResolveAddressTime(SocketAddress remoteAddress, Duration time, String status) {

    }
}
