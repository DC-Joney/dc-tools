package com.dc.tools.spring.http;

import com.alibaba.fastjson.JSONObject;
import com.dc.cache.Cache;
import com.dc.cache.CacheKey;
import com.dc.tools.common.utils.CloseTasks;
import com.dc.tools.common.utils.WriteContext;
import com.dc.tools.io.serilizer.ProtoStuffUtils;
import com.dc.tools.spring.http.exception.HttpRequestException;
import com.dc.tools.spring.utils.AopTargetUtils;
import com.dc.tools.spring.utils.BusinessAssert;
import com.dc.tools.spring.utils.JsonMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.http.HttpStatus;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import reactor.netty.Connection;
import reactor.netty.NettyOutbound;
import reactor.netty.http.client.HttpClientRequest;
import reactor.netty.http.client.HttpClientResponse;
import reactor.util.context.Context;
import reactor.util.context.ContextView;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


/**
 * 好未来接口请求的抽象实现
 *
 * @param <REQ>  request
 * @param <RESP> response
 * @param <C>    转换以后得结果
 * @author zy
 */
@Slf4j
public abstract class AbstractHttpClient<REQ, RESP, C> implements AsyncClient<REQ, C>, ApplicationContextAware {

    /**
     * Cache time key for request trace
     */
    private static final String CACHE_TIME_KEY = "result_cache";

    /**
     * 用于超时时间的定时任务线程池
     */
    private static final ScheduledExecutorService TIMEOUT_POOL = Executors.newScheduledThreadPool(2);

    /**
     * response code attribute
     */
    public static final String RESPONSE_CODE_ATTRIBUTE = "code";

    public static final int SUCCESS_CODE = 20000;


    @Setter
    @Getter
    private HttpClientDecoration decoration;

    @Setter
    @Getter
    private JsonMapper jsonMapper;

    /**
     * 请求拦截器 用于拦截request 以及 对返回数据做处理
     */
    private List<RequestInterceptor<HttpRequest<REQ>, RESP>> requestInterceptors = new ArrayList<>();

    @Setter
    private ApplicationContext applicationContext;

    @Setter
    private ResultConverter<RESP, Mono<C>> converter;

    /**
     * 用于添加需要关闭的任务
     */
    @Getter
    final CloseTasks closeTasks = new CloseTasks();

    @Setter
    private Cache<CacheKey, C> requestCache;

    /**
     * 用于超时时间
     */
    @Setter
    private Scheduler timeoutScheduler;

    @Override
    public void start() throws Exception {
        if (decoration == null) {
            decoration = DefaultHttpClientDecoration.getInstance();
        }

        if (jsonMapper == null) {
            jsonMapper = JsonMapper.nonNullMapper();
        }

        if (requestCache == null) {
            requestCache = new EmptyCache<>();
        }

        if (timeoutScheduler == null) {
            timeoutScheduler = Schedulers.fromExecutorService(TIMEOUT_POOL);
        }

        //添加缓存清除监听器，用于排查问题
        requestCache.addRemoveListener(cacheValue -> {
            CacheKey cacheKey = cacheValue.getKey();
            REQ param = decodeCacheKey(cacheKey);
            log.info("Request {} cache will be remove, image info is: {}", clientName(), param);
            //当缓存被remove时, 需要把引用的byteBuf一起删除
            cacheKey.release();
        });

        //对拦截器进行排序并且通过刷新所有的数据值
        requestInterceptors = requestInterceptors.stream()
                .map(this::autowireBean)
                .sorted(AnnotationAwareOrderComparator.INSTANCE)
                .collect(Collectors.toList());

        closeTasks.addTask(decoration::close, "Close http client for {}", decoration.name());
    }


    @SuppressWarnings("unchecked")
    protected <T> T autowireBean(T interceptor) {
        //如果applicationContext为空则直接忽略
        if (applicationContext != null) {
            applicationContext.getAutowireCapableBeanFactory()
                    .autowireBeanProperties(interceptor, AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE, false);

            return (T) applicationContext.getAutowireCapableBeanFactory()
                    .initializeBean(interceptor, interceptor.getClass().getName());
        }

        return interceptor;
    }

    @Override
    public void stop() {
        closeTasks.close();
    }

    /**
     * 请求好未来的客户端名称
     *
     * @return 接口名称
     */
    public abstract String clientName();

    @Override
    public Publisher<HttpResponse<C>> requestAsync(HttpRequest<REQ> param, Duration cacheTime) {
        //获取当前对象的代理对象
        AbstractHttpClient<REQ, RESP, C> proxyClient = AopTargetUtils.getProxy(this);
        //在请求之前那判断是否需要对数据进行更改
        return fromCache(param)
                .map(HttpResponse::successResponse)
                .transform(responseMono -> {
                    //如果超时时间 > 0 表示需要设置超时时间
                    if (param.getResourceTimeoutMills() > 0) {
                        return responseMono.timeout(Duration.ofMillis(param.getResourceTimeoutMills()), timeoutScheduler);
                    }
                    return responseMono;
                })
                .doOnError(ex -> log.error("Request to {} error, cause is: ", clientName(), ex))
                //当异常信息为TalRequestException时，则直接返回jsonResult
                .onErrorResume(HttpRequestException.class, ex -> Mono.just(HttpResponse.errorResponse(ex.getResultCode(), ex.getJsonResult())))
                .onErrorReturn(HttpResponse.errorResponse())
                .contextWrite(context -> addContext(cacheTime, proxyClient, context));
    }


    /**
     * 从缓存中获取数据
     *
     * @param param 请求参数
     */
    @SuppressWarnings("unchecked")
    public Mono<C> fromCache(HttpRequest<REQ> param) {
        return Mono.deferContextual(contextView -> {
            return Mono.just(param)
                    .map(HttpRequest::getBody)
                    //从缓存中获取数据
                    .flatMap(this::getCache)
                    //获取不到数据则从源头拉取
                    .switchIfEmpty(Mono.defer(() -> requestHttp(param)
                            //对返回结果进行转换
                            .transform(respMono -> converter == null ? (Mono<C>) respMono
                                    : respMono.flatMap(resp -> converter.convert(resp, contextView)))))
                    //请求成功后添加缓存
                    .doOnSuccess(result -> addCache(param, result, contextView));
        });
    }


    /**
     * 在执行链路时将上下文信息添加到整个链路中
     *
     * @param cacheTime   缓存的时间
     * @param proxyClient 当前执行的对象
     * @param context     已有的上下文对象
     */
    protected Context addContext(Duration cacheTime, AbstractHttpClient<REQ, RESP, C> proxyClient, Context context) {
        //获取新的context
        Context newCtx = context.put(this.getClass(), proxyClient).put(CACHE_TIME_KEY, cacheTime);
        //创建用于interceptor的上下文，保证每次请求都会产生一个新的requestContext
        WriteContext requestContext = new WriteContext();
        requestContext.putAll(context);
        //避免循环依赖
        return newCtx.put(WriteContext.class, requestContext);
    }


    /**
     * 构造缓存key
     *
     * @param request 请求参数
     * @return 返回构造的缓存key
     */
    protected CacheKey buildCacheKey(REQ request) {
        ByteBuf byteBuf = ProtoStuffUtils.serializeBuf(request);
        return CacheKey.toCacheKey(byteBuf);
    }



    /**
     * @param cacheKey 缓存key
     * @return 返回缓存的请求参数
     */
    protected REQ decodeCacheKey(CacheKey cacheKey) {
        return ProtoStuffUtils.deserialize(cacheKey.getByteBuf());
    }

    /**
     * 获取缓存
     *
     * @param request 请求参数
     */
    protected Mono<C> getCache(REQ request) {
        //这里不会占用内存空间，因为在getCache方法执行完成后就会释放
        CacheKey cacheKey = buildCacheKey(request);
        C result = requestCache.get(cacheKey);
        if (result != null) {
            log.warn("Find result from image cache, cache result info is: {}", showCacheResult(result));
        }

        return Mono.justOrEmpty(result);
    }

    /**
     * 将结果添加到缓存
     *
     * @param log2Request 请求参数
     * @param result      返回结果
     */
    protected void addCache(HttpRequest<REQ> log2Request, C result, ContextView context) {
        CacheKey cacheKey = buildCacheKey(log2Request.getBody());
        Duration cacheTime = context.get(CACHE_TIME_KEY);
        if (cacheTime != Duration.ZERO) {
            long seconds = cacheTime.getSeconds();
            requestCache.put(cacheKey, result, seconds, TimeUnit.SECONDS);
        }

    }

    /**
     * 打印缓存信息时，可能存在像图片base64这种数据的情况，所以需要对打印的结果进行简化
     *
     * @param result 请求的结果
     * @return 返回缓存的数据
     */
    protected String showCacheResult(C result) {
        return result.toString();
    }


    /**
     * requestHttp 方法不会吞掉相应的异常，而是会将该异常向上抛出去，该方法主要用于其他client调用
     *
     * @param log2Request 请求参数
     */
    @SuppressWarnings("unchecked")
    public Mono<RESP> requestHttp(HttpRequest<REQ> log2Request) {
        return Mono.deferContextual(contextView -> {
            //获取当前对象的代理对象，这里只能从context中进行获取，因为如果通过AopTargetUtils获取可能由于跨线程的问题会导致代理对象丢失
            AbstractHttpClient<REQ, RESP, C> proxyClient = contextView.getOrDefault(this.getClass(), this);

            return Mono.just(log2Request)
                    .flatMap(this::beforeExecute)
                    //请求好未来接口
                    .flatMap(proxyClient::requestTalOrigin)
                    //处理返回数据
                    .flatMap(this::afterExecute);
        });
    }


    /**
     * requestTalOrigin 是拿到请求好未来最原始的参数和返回结果
     *
     * @param log2Request 请求参数
     */
    protected Mono<RESP> requestTalOrigin(HttpRequest<REQ> log2Request) {
        return Mono.justOrEmpty(log2Request.getBody())
                .transformDeferredContextual((requestMono, contextView) -> requestMono.flatMap(request -> actualRequest(request, contextView)))
                //将结果转为对应的response
                .map(this::convertToResponse)
                .doOnError(ex -> log.error("Request {} error, cause is: ", clientName(), ex));
    }


    private Mono<JSONObject> actualRequest(REQ actualRequest, ContextView contextView) {
        String talUrl = getRequestUrl(actualRequest, contextView);
        return decoration.getHttpClient()
                .post()
                .uri(talUrl)
                .send((request, outbound) -> sendBody(request, outbound, actualRequest, contextView))
                .responseConnection((response, connection) -> handleResult(response, connection, contextView, actualRequest))
                .next();

    }

    /**
     * 实际写入requestBody
     *
     * @param request       http request
     * @param outbound      netty outgoing
     * @param actualRequest 实际请求的requestBody
     * @param contextView   context info
     */
    protected Publisher<Void> sendBody(HttpClientRequest request, NettyOutbound outbound, REQ actualRequest, ContextView contextView) {
        request.requestHeaders().add(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON);
        return outbound.send(decodeBody(actualRequest, outbound.alloc())).then();
    }

    /**
     * 获取请求的url
     *
     * @param request     请求参数
     * @param contextView 请求的context
     * @return request url
     */
    protected abstract String getRequestUrl(REQ request, ContextView contextView);


    protected Publisher<ByteBuf> decodeBody(REQ req, ByteBufAllocator allocator) {
        try {
            byte[] bytes = getJsonMapper().getMapper().writeValueAsBytes(req);
            //通过堆外内存包装,避免过多的占用堆内存, 不要在这里直接使用JSON.toJSONString, 因为本身返回的string就占用内存，尽量
            ByteBuf byteBuf = allocator.directBuffer(bytes.length);
            return Mono.just(byteBuf).doOnNext(buffer -> buffer.writeBytes(bytes));
        } catch (JsonProcessingException e) {
            return Mono.error(new HttpRequestException(e, "Response {} json parse error, cause is: {}", clientName(), e.getMessage()));
        }
    }


    /**
     * 处理返回结果
     *
     * @param response   response
     * @param connection response 对应的connection
     * @param context    请求上下文
     * @param request    请求参数
     */
    protected Publisher<JSONObject> handleResult(HttpClientResponse response, Connection connection,
                                                 ContextView context, REQ request) {
        HttpStatus httpStatus = HttpStatus.valueOf(response.status().code());

        //如果http状态码非2xx，那么返回错误信息
        if (!httpStatus.is2xxSuccessful()) {
            //如果返回的状态码是401的话，解析返回结果并且将其错误信息进行包装
            return Mono.just(httpStatus)
                    .<JSONObject>flatMap(code -> {
                        //获取返回的错误结果信息，并且将其包装为json返回
                        return connection.inbound()
                                .receive()
                                .aggregate()
                                .asString()
                                .doOnNext(jsonResult -> log.error("Request {} status code is {}, cause is: {}", code.value(), clientName(), jsonResult))
                                .map(JSONObject::parseObject)
                                .flatMap(json -> Mono.error(new HttpRequestException(json.toJSONString(), json.getInteger(RESPONSE_CODE_ATTRIBUTE),
                                        "Call remote {} error, cause is: response code not success {}, cause is: {}", clientName(), response.status().code(), json)));

                    })
                    //返回默认的错误结果信息
                    .switchIfEmpty(Mono.error(new HttpRequestException("Call remote {} error, cause is: response code not success {}", clientName(), response.status().code())));
        }

        return connection.inbound()
                .receive()
                .aggregate()
                .asString()
                .map(jsonResult -> parseResult(request, jsonResult));
    }

    /**
     * 解析返回结果
     *
     * @param request 请求参数
     * @param result  返回结果
     */
    protected JSONObject parseResult(REQ request, String result) {
        JSONObject jsonObject = JSONObject.parseObject(result);
        if (!jsonObject.containsKey(RESPONSE_CODE_ATTRIBUTE) || jsonObject.getInteger(RESPONSE_CODE_ATTRIBUTE) != SUCCESS_CODE) {
            Integer resCode = jsonObject.getInteger(RESPONSE_CODE_ATTRIBUTE);
            throw new HttpRequestException(result, resCode, "Request {} error, detail is: {}", clientName(), jsonObject.toJSONString());
        }

        return jsonObject;
    }

    /**
     * 将json  转为具体的对象
     *
     * @param jsonObject json
     * @return 将jsonObject 转换成具体的对象
     */
    protected abstract RESP convertToResponse(JSONObject jsonObject);

    /**
     * 在请求完成以后对数据进行修改处理
     *
     * @param response response object
     */
    protected Mono<RESP> afterExecute(RESP response) {
        return Mono.deferContextual(contextView -> {
            Mono<RESP> newJson = Mono.just(response);
            for (RequestInterceptor<HttpRequest<REQ>, RESP> requestInterceptor : requestInterceptors) {
                newJson = newJson.flatMap(json -> requestInterceptor.afterResponse(response, contextView));
            }

            return newJson;
        });
    }

    /**
     * 在执行具体的请求之前对业务进行处理
     *
     * @param log2Request 请求对象
     */
    protected <R> Mono<HttpRequest<REQ>> beforeExecute(HttpRequest<REQ> log2Request) {
        return Mono.deferContextual(contextView -> {
            Mono<HttpRequest<REQ>> convertMono = Mono.just(log2Request);
            for (RequestInterceptor<HttpRequest<REQ>, RESP> requestInterceptor : requestInterceptors) {
                convertMono = convertMono.flatMap(request -> requestInterceptor.beforeRequest(request, contextView));
            }

            return convertMono;
        });

    }

    /**
     * 添加请求拦截器
     *
     * @param interceptor 拦截器
     * @return 返回新的入参
     */
    public AbstractHttpClient<REQ, RESP, C> addInterceptor(RequestInterceptor<HttpRequest<REQ>, RESP> interceptor) {
        requestInterceptors.add(interceptor);
        return this;
    }

    /**
     * 添加请求拦截器
     *
     * @param interceptor 拦截器
     * @return 返回新的入参
     */
    public AbstractHttpClient<REQ, RESP, C> addBeforeInterceptor(BeforeRequestInterceptor<HttpRequest<REQ>> interceptor) {
        BusinessAssert.notNull(interceptor, "Before interceptor instance must not be null");
        requestInterceptors.add(new BeforeRequestInterceptorAdaptor<>(interceptor));
        return this;
    }


    /**
     * 添加请求拦截器
     *
     * @param interceptor 拦截器
     * @return 返回新的入参
     */
    public AbstractHttpClient<REQ, RESP, C> addAfterInterceptor(AfterRequestInterceptor<RESP> interceptor) {
        BusinessAssert.notNull(interceptor, "After interceptor instance must not be null");
        requestInterceptors.add(new AfterRequestInterceptorAdaptor<>(interceptor));
        return this;
    }

    /**
     * add interceptor list
     *
     * @param interceptors interceptors
     */
    public AbstractHttpClient<REQ, RESP, C> addInterceptors(List<RequestInterceptor<HttpRequest<REQ>, RESP>> interceptors) {
        requestInterceptors.addAll(interceptors);
        return this;
    }


    @AllArgsConstructor
    class BeforeRequestInterceptorAdaptor<NEW_REQ, NEW_RESP> implements RequestInterceptor<NEW_REQ, NEW_RESP>, InitializingBean {

        private BeforeRequestInterceptor<NEW_REQ> interceptor;

        @Override
        public void afterPropertiesSet() throws Exception {
            interceptor = AbstractHttpClient.this.autowireBean(interceptor);
        }

        @Override
        public Mono<NEW_RESP> afterResponse(NEW_RESP response, ContextView context) {
            return Mono.just(response);
        }

        @Override
        public Mono<NEW_REQ> beforeRequest(NEW_REQ request, ContextView context) {
            return interceptor.beforeRequest(request, context);
        }
    }


    @AllArgsConstructor
    class AfterRequestInterceptorAdaptor<NEW_REQ, NEW_RESP> implements RequestInterceptor<NEW_REQ, NEW_RESP>, InitializingBean {

        private AfterRequestInterceptor<NEW_RESP> interceptor;

        @Override
        public void afterPropertiesSet() throws Exception {
            interceptor = AbstractHttpClient.this.autowireBean(interceptor);
        }

        @Override
        public Mono<NEW_RESP> afterResponse(NEW_RESP response, ContextView context) {
            return interceptor.afterResponse(response, context);
        }

        @Override
        public Mono<NEW_REQ> beforeRequest(NEW_REQ request, ContextView context) {
            return Mono.just(request);
        }
    }
}
