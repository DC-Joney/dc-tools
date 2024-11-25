package com.dc.tools.spring.http;

import reactor.core.publisher.Mono;
import reactor.util.context.ContextView;

/**
 * Before Request Interceptor
 *
 * @author zy
 * @param <REQ>
 */
public interface BeforeRequestInterceptor<REQ> {

    /**
     * @param request 请求的参数
     * @param context 用于只读的context
     * @return 返回修改之后的请求参数
     */
    Mono<REQ> beforeRequest(REQ request, ContextView context);
}
