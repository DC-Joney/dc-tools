package com.dc.tools.spring.http;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.RatioGauge;
import com.codahale.metrics.Timer;
import com.dc.tools.common.annotaion.NonNull;
import com.dc.tools.common.utils.CloseTasks;
import io.netty.channel.AdaptiveRecvByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.WriteBufferWaterMark;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.util.AttributeKey;
import io.netty.util.HashedWheelTimer;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import reactor.netty.Connection;
import reactor.netty.ConnectionObserver;
import reactor.netty.NettyPipeline;
import reactor.netty.http.HttpProtocol;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionPoolMetrics;
import reactor.netty.resources.ConnectionProvider;
import reactor.netty.resources.LoopResources;

import java.io.IOException;
import java.net.SocketAddress;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * Default http client decoration
 *
 * @author zhangyang
 */
@Slf4j
public class DefaultHttpClientDecoration implements HttpClientDecoration {


    private static final DefaultHttpClientDecoration INSTANCE = new DefaultHttpClientDecoration();

    private static final Integer DEFAULT_SELECTOR_COUNT = Runtime.getRuntime().availableProcessors() * 2;

    public static final String NETTY_STATICS_HANDLER = NettyPipeline.LEFT + "STATICS_HANDLER";

    /**
     * 用于计算请求耗时
     */
    private static final AttributeKey<LocalDateTime> START_TIME_KEY = AttributeKey.valueOf("startTime");
    /**
     * 用于监控Request 与 Response之间的交互
     */
    private static final AttributeKey<Timer.Context> TIMER_KEY = AttributeKey.valueOf("timerContext");

    /**
     * 第三方请求消耗标准时间
     */
    private static final Duration STANDARD_DURATION = Duration.ofSeconds(1);

    /**
     * Default http client name
     */
    public static final String HTTP_CLIENT_NAME = "DEFAULT";

    private static final String POOL_NAME = "DEFAULT_POOL";

    private final CloseTasks closeTasks = new CloseTasks();

    private final HttpClient httpClient;

    private final Integer selectorCount;

    private final String poolName;

    public DefaultHttpClientDecoration() {
        this(POOL_NAME, 500);
    }

    public DefaultHttpClientDecoration(String poolName, int maxConnections) {
        this.selectorCount = Runtime.getRuntime().availableProcessors() * 2;
        this.httpClient = createDefaultHttpClient(poolName, maxConnections);
        this.poolName = poolName;
    }

    public DefaultHttpClientDecoration(String poolName, int maxConnections, Integer selectorCount) {
        this.selectorCount = selectorCount != null && selectorCount > 0 ? selectorCount : DEFAULT_SELECTOR_COUNT;
        this.httpClient = createDefaultHttpClient(poolName, maxConnections);
        this.poolName = poolName;
    }


    @Override
    public HttpClient getHttpClient() {
        return httpClient;
    }

    @Override
    public void close() throws IOException {
        closeTasks.close();
    }


    @Override
    public String name() {
        return poolName;
    }

    public static DefaultHttpClientDecoration getInstance() {
        return INSTANCE;
    }

    /**
     * 创建默认的Http client 连接池
     *
     * @param maxConnections 最大连接数
     * @return http client pool
     */
    public HttpClient createDefaultHttpClient(String poolName, int maxConnections) {
        //针对慢请求直方图统计
        Timer timer = HttpClientMetricRegistry.getTimer(poolName + ":SLOW_REQUEST");
        //添加请求错误统计
        Counter requestError = HttpClientMetricRegistry.getCounter(poolName + ":REQUEST_ERROR");
        Counter responseError = HttpClientMetricRegistry.getCounter(poolName + ":RESPONSE_ERROR");


        //创建连接池
        ConnectionProvider provider = createProvider(poolName, maxConnections);
        closeTasks.addTask(provider::dispose, "Close http connection provider");

        LoopResources loopResources = LoopResources.create(poolName, selectorCount, true);


        HttpClient httpClient = HttpClient.create(provider)
                .compress(true)
                //添加长连接
                .keepAlive(true)
                //添加Netty的LogHandler
                //.wiretap(true)
                .httpResponseDecoder(decoder -> decoder.maxChunkSize(1 << 16).initialBufferSize(1 << 10))
                .doOnRequest((request, connection) -> log.info("Request headers: {}", request.requestHeaders()))
                .observe(new DefaultConnectionObserver())
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 30000)
                .option(ChannelOption.TCP_NODELAY, true)
                //发送缓存区默认为8M
                //这里考虑到图片的大小问题，可以改为4M
                .option(ChannelOption.SO_SNDBUF, 8388608)
                //接受缓存区为4M,完全够用了, 因为需要考虑到弯曲矫正的问题所以设置偏大
                //TODO: 这里是否可以减少到256KB
                .option(ChannelOption.SO_RCVBUF, 4194304)
                //高低水位设置，lowWater=512KB, highWater=64M
                //低水位：当缓存中的数据大小低于低水位时表示可以继续写入
                //高水位：缓存中的数据达到高水位时表示不可以继续写入了
                .option(ChannelOption.WRITE_BUFFER_WATER_MARK, new WriteBufferWaterMark(524288, 67108864))
                //用于接受数据的缓冲区最大为4M
                //初始化缓冲区默认为2048字节，最大为8M，初始化为1M
                .option(ChannelOption.RCVBUF_ALLOCATOR, new AdaptiveRecvByteBufAllocator())
                .runOn(loopResources, true)
                .headers(headers -> headers.set(HttpHeaderNames.CONTENT_TYPE, "application/json"))
                .doOnRequest((req, connection) -> {
                    Channel channel = connection.channel();
                    //添加开始时间
                    channel.attr(START_TIME_KEY).set(LocalDateTime.now());
                    channel.attr(TIMER_KEY).set(timer.time());
                })
                .doOnResponse((res, connection) -> {
                    LocalDateTime startTime = connection.channel().attr(START_TIME_KEY).getAndSet(null);
                    Timer.Context context = connection.channel().attr(TIMER_KEY).getAndSet(null);
                    LocalDateTime endTime = LocalDateTime.now();
                    Duration timeConsuming = Duration.between(startTime, endTime);
                    //如果第三方请求时间 超过标准耗时，则进行累加
                    if (timeConsuming.compareTo(STANDARD_DURATION) > 0) {
                        log.error("Slow request time is: {}", Duration.between(startTime, endTime));
                    }

                    context.stop();
                })
                .doOnConnected(connection -> log.info("Connected to remote server: {}", connection.channel().localAddress()))
                .doOnDisconnected(connection -> log.info("Disconnected tcp remote connection:  {}, channel info: {}", connection.channel().localAddress(), connection.channel().isActive()))
                .doOnChannelInit((connectionObserver, channel, remoteAddress) -> {
                    channel.pipeline().addFirst(NETTY_STATICS_HANDLER, ChannelReadTimeStaticsHandler.INSTANCE);
                })
                .doOnChannelInit((connectionObserver, channel, remoteAddress) -> {
                    //添加channel关闭事件
                    channel.closeFuture().addListener(new GenericFutureListener<Future<? super Void>>() {
                        @Override
                        public void operationComplete(Future<? super Void> future) throws Exception {
                            channel.closeFuture().removeListener(this);
                            Throwable cause = future.cause();
                            if (cause != null) {
                                log.error("Close channel failed, address is: {},  cause is:", channel.localAddress(), cause);
                                return;
                            }

                            log.info("Close channel succeeded, address is: {}", channel.localAddress());
                        }
                    });
                })
                .doOnRequestError((request, throwable) -> requestError.inc())
                .doOnResponseError((request, throwable) -> responseError.inc())
                .doOnResponse((resp, connection) -> {
                    HttpStatus httpStatus = HttpStatus.resolve(resp.status().code());
                    if (httpStatus != null && httpStatus.isError()) {
                        //response请求失败时进行累加
                        responseError.inc();
                    }
                })
                .noSSL()
                .noProxy()
                .metrics(true, () -> new HttpClientMetric(poolName))
//                .wiretap("reactor.netty", LogLevel.DEBUG)
                .protocol(HttpProtocol.HTTP11);

        httpClient.warmup()
                .doOnTerminate(() -> log.info("Warming up successful connection"))
                .subscribe();

        return httpClient;
    }


    private ConnectionProvider createProvider(String poolName, int maxConnections) {
        return ConnectionProvider
                .builder(poolName)
                //连接池最大连接数
                .maxConnections(maxConnections)
                //连接最大空闲时间，如果超过3个小时还是没有被用到则会关闭连接
                .maxIdleTime(Duration.ofHours(3))
                //从连接池等待获取连接的队列大小
                .pendingAcquireMaxCount(500)
                //用于定时清除连接池中的可删除的连接
                .evictInBackground(Duration.ofMinutes(5))

                //TODO: reactor bug，开启监控会导致cpu飙升
                // bug 原因：https://github.com/reactor/reactor-netty/issues/1790
                // 解决方法: https://github.com/reactor/reactor-netty/pull/1797
//                .metrics(true, () -> new ConnectionMetric(maxConnections))
                //获取连接的超时时间, 默认为1s
                .pendingAcquireTimeout(Duration.ofMillis(ConnectionProvider.DEFAULT_POOL_ACQUIRE_TIMEOUT))
                //连接最大存活时间, 默认为3小时 既 accessTime + expireTime
//                .maxLifeTime(Duration.ofHours(3))
                //当连接池调用dispose 或者是disposeLater时，再从连接池中获取连接会直接触发异常，对于需要获取连接完成任务的情况，提供了部分延迟事件
                //.disposeTimeout(Duration.ZERO)
                .build();
    }

    static class DefaultConnectionObserver implements ConnectionObserver {

        @Override
        public void onStateChange(@NonNull Connection connection, @NonNull State newState) {

            if (newState == State.CONNECTED) {
                log.info("Connected channel: {}", connection.channel().localAddress());
                connection.onTerminate().subscribe(unused -> log.info("Channel disconnected: {}", connection.address()));
            }

            if (newState == State.ACQUIRED) {
                log.info("Acquired channel: {}", connection.channel().localAddress());
            }

            if (newState == State.RELEASED) {
                log.info("Released channel: {}", connection.channel().localAddress());
            }

            if (newState == State.DISCONNECTING) {
                log.info("DISCONNECTING channel: {}", connection.channel().localAddress());
            }

        }
    }

    static class HttpClientMetric extends HttpClientMetricsRecorderAdaptor {

        private final Histogram dataSentHistogram;
        private final Histogram dataReceiveHistogram;
        private final Histogram dataResponseHistogram;

        public HttpClientMetric(String poolName) {
            this.dataSentHistogram = HttpClientMetricRegistry.getHistogram(poolName + ":DATA_SENT");
            this.dataReceiveHistogram = HttpClientMetricRegistry.getHistogram(poolName + ":DATA_RECEIVE");
            this.dataResponseHistogram = HttpClientMetricRegistry.getHistogram(poolName + ":REQUEST_RESPONSE");
        }

        @Override
        public void incrementErrorsCount(SocketAddress remoteAddress) {
            super.incrementErrorsCount(remoteAddress);
        }

        @Override
        public void recordDataSentTime(SocketAddress remoteAddress, String uri, String method, Duration time) {
            dataSentHistogram.update(time.toMillis());
        }


        @Override
        public void recordDataReceivedTime(SocketAddress remoteAddress, String uri, String method, String status, Duration time) {
            dataReceiveHistogram.update(time.toMillis());
        }

        @Override
        public void recordResponseTime(SocketAddress remoteAddress, String uri, String method, String status, Duration time) {
            dataResponseHistogram.update(time.toMillis());
        }
    }

    /**
     * 连接池监控
     */
    static class ConnectionMetric implements ConnectionProvider.MeterRegistrar {

        /**
         * 最大连接数
         */
        private final int maxConnections;

        private static final HashedWheelTimer HISTOGRAM_TIMER = new HashedWheelTimer();

        public ConnectionMetric(int maxConnections) {
            this.maxConnections = maxConnections;
        }

        @Override
        public void registerMetrics(@NonNull String poolName, @NonNull String id, @NonNull SocketAddress remoteAddress, @NonNull ConnectionPoolMetrics metrics) {
            HttpClientMetricRegistry.addIfAbsent(poolName + ":idleCount", metrics::idleSize);
            HttpClientMetricRegistry.addIfAbsent(poolName + ":allocatedCount", metrics::allocatedSize);
            HttpClientMetricRegistry.addIfAbsent(poolName + ":acquiredCount", metrics::acquiredSize);
            HttpClientMetricRegistry.addIfAbsent(poolName + ":pendingAcquireSize", metrics::pendingAcquireSize);
            HttpClientMetricRegistry.addRatioGaugeIfAbsent(poolName + ":acquiredRatio", () -> RatioGauge.Ratio.of(metrics.acquiredSize(), maxConnections));
            //添加pendingAcquire指标统计
            Histogram histogram = HttpClientMetricRegistry.getHistogram(poolName + ":pendingAcquire");
            //每500ms更新下当前的值，以保证数据可以更准确的统计
            HISTOGRAM_TIMER.newTimeout(timeout -> histogram.update(metrics.pendingAcquireSize()), 500, TimeUnit.MILLISECONDS);
        }
    }


}
