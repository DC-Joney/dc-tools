package com.dc.tools.spring.http;

import com.dc.tools.spring.utils.AopTargetUtils;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * 异步请求接口
 *
 * @author zhangyang
 */
public interface AsyncClient<REQ, RES> extends LiftCycle {

    /**
     * 异步请求接口
     *
     * @param param     请求参数
     * @param cacheTime 缓存结果的时间，{@linkplain Duration#ZERO} 表示不缓存
     * @return 返回请求的结果
     */
    Publisher<HttpResponse<RES>> requestAsync(HttpRequest<REQ> param, Duration cacheTime);


    /**
     * Alias for {@link AsyncClient#requestAsync(HttpRequest, Duration)} method
     * <p>
     * 调用当前方法不会缓存请求后的结果
     * </p>
     *
     * @param param 请求参数
     * @return 返回请求的结果
     */
    default Publisher<HttpResponse<RES>> requestAsync(HttpRequest<REQ> param) {
        AsyncClient<REQ, RES> asyncClient = AopTargetUtils.getProxy(this);
        return asyncClient.requestAsync(param, Duration.ZERO);
    }

    /**
     * 异步请求接口
     *
     * @param param     请求参数
     * @param cacheTime 缓存结果的时间，{@linkplain Duration#ZERO} 表示不缓存
     * @return 返回请求的结果
     */
    default Flux<HttpResponse<RES>> requestManyAsync(HttpRequest<REQ> param, Duration cacheTime) {
        AsyncClient<REQ, RES> asyncClient = AopTargetUtils.getProxy(this);
        return Flux.from(asyncClient.requestAsync(param, cacheTime));
    }

    /**
     * 异步请求接口
     *
     * @param param 请求参数
     * @return 返回请求的结果
     */
    default Flux<HttpResponse<RES>> requestManyAsync(HttpRequest<REQ> param) {
        AsyncClient<REQ, RES> asyncClient = AopTargetUtils.getProxy(this);

        return Flux.from(requestAsync(param));
    }


    /**
     * 异步请求接口
     *
     * @param param     请求参数
     * @param cacheTime 缓存结果的时间，{@linkplain Duration#ZERO} 表示不缓存
     * @return 返回请求的结果
     */
    default Mono<HttpResponse<RES>> requestSingleAsync(HttpRequest<REQ> param, Duration cacheTime) {
        AsyncClient<REQ, RES> asyncClient = AopTargetUtils.getProxy(this);
        return Mono.fromDirect(asyncClient.requestAsync(param, cacheTime));
    }

    /**
     * 异步请求接口
     *
     * @param param 请求参数
     * @return 返回请求的结果
     */
    default Mono<HttpResponse<RES>> requestSingleAsync(HttpRequest<REQ> param) {
        AsyncClient<REQ, RES> asyncClient = AopTargetUtils.getProxy(this);
        return Mono.fromDirect(asyncClient.requestAsync(param));
    }


}
