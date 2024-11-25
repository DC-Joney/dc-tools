package com.dc.tools.spring.http;

import reactor.core.publisher.Mono;
import reactor.util.context.ContextView;

/**
 * 请求拦截器
 *
 * @author zy
 * @param <RESP>
 */
public interface AfterRequestInterceptor<RESP> {

    /**
     * 在执行完完成后对返回结果做处理
     *
     * @param context  用于只读的context
     * @param response 返回结果
     */
    Mono<RESP> afterResponse(RESP response, ContextView context);
}
