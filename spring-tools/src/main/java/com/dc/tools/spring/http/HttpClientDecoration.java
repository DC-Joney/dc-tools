package com.dc.tools.spring.http;

import reactor.netty.http.client.HttpClient;

import java.io.Closeable;

/**
 * 用于包装 {@link HttpClient} 实现
 *
 * @author zhangyang
 */
public interface HttpClientDecoration extends Closeable {

    /**
     *
     * Returns the http client instance
     *
     * @return http client instance
     */
    HttpClient getHttpClient();

    /**
     * Returns http client name
     *
     * @return http client name
     */
    String name();
}
