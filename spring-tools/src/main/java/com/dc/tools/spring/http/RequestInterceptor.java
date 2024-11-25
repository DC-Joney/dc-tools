package com.dc.tools.spring.http;

/**
 * 添加请求响应的拦截器 进行修改
 *
 * @param <REQ>
 * @param <RESP>
 * @author zy
 */
public interface RequestInterceptor<REQ, RESP> extends BeforeRequestInterceptor<REQ>, AfterRequestInterceptor<RESP> {


}
