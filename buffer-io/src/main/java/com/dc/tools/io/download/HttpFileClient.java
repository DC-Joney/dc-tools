package com.dc.tools.io.download;

import com.dc.pool.thread.NamedThreadFactory;
import com.dc.pool.thread.ThreadPoolUtil;

import com.dc.tools.common.exception.ExceptionManager;
import com.dc.tools.common.utils.CloseTasks;
import com.dc.tools.common.utils.ShareTempDir;
import com.dc.tools.common.utils.StringUtils;
import com.google.common.net.HttpHeaders;
import io.netty.buffer.ByteBuf;
import io.netty.util.ReferenceCounted;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import reactor.netty.http.HttpProtocol;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.client.HttpClientResponse;
import reactor.netty.resources.ConnectionProvider;
import reactor.util.context.Context;

import javax.annotation.concurrent.ThreadSafe;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 多线程文件下载
 *
 * @author zhangyang
 */
@Slf4j
@ThreadSafe
@FieldDefaults(level = AccessLevel.PRIVATE)
public class HttpFileClient {

    private static final HttpFileClient INSTANCE = new HttpFileClient();

    /**
     * 多线程下载文件的大小阈值,当大于这个数量时才会进行下载
     */
    private static final int PARALLEL_FILE_SIZE = 1 << 20;

    /**
     * 并行的线程数量
     */
    private static final int THREAD_SIZE = 1 << 4;

    private static final int SPLIT_COUNT = 3;

    /**
     * 文件切分的数量，默认为16
     */
    private static final int DEFAULT_FILE_SPLIT_COUNT = 1 << SPLIT_COUNT;


    public static final String DOWNLOAD_PATH = "DOWNLOAD_FILE_PATH";

    /**
     * 用于同步生成临时目录,惰性加载
     */
    final ShareTempDir shareTempDir = new ShareTempDir(false, "download");

    /**
     * 用于远程请求Http
     */
    private HttpClient httpClient;

    /**
     * 当文件需要拆分为多个chunk时，通过多线程进行下载
     */
    Scheduler downloadScheduler;

    /**
     * 用于提供连接池服务
     */
    ConnectionProvider provider;

    private int splitCount = DEFAULT_FILE_SPLIT_COUNT;

    public HttpFileClient(Scheduler scheduler) {
        this.downloadScheduler = scheduler;
        init();
    }

    public HttpFileClient(Scheduler scheduler, int splitCount) {
        this.downloadScheduler = scheduler;
        this.splitCount = splitCount;
        init();
    }

    public HttpFileClient(int splitCount) {
        this.splitCount = splitCount;
        init();
    }


    public HttpFileClient() {
        init();
    }


    public static HttpFileClient getInstance() {
        return INSTANCE;
    }

    public static HttpFileClient create(Scheduler scheduler, int splitCount) {
        return new HttpFileClient(scheduler, splitCount);
    }

    public static HttpFileClient create(Scheduler scheduler) {
        return new HttpFileClient(scheduler);
    }


    /**
     * 初始化HttpClient
     */
    public void init() {
        provider = ConnectionProvider
                .builder("HttpFile Client")
                .fifo()
                //连接池最大连接数
                .maxConnections(100)
                //连接最大空闲时间，如果超过1个小时还是没有被用到则会关闭连接
                .maxIdleTime(Duration.ofHours(1))
                //                .pendingAcquireMaxCount(10)
                //用于定时清除连接池中的可删除的连接
                //                .evictInBackground(Duration.ofMinutes(5))
                //获取连接的超时时间
                .pendingAcquireTimeout(Duration.ofSeconds(60))
                .pendingAcquireMaxCount(60)

                //连接最大存活时间
//                .maxLifeTime(Duration.ofMillis(60))

                //当连接池调用dispose 或者是disposeLater时，再从连接池中获取连接会直接触发异常，对于需要获取连接完成任务的情况，提供了部分延迟事件
                //                .disposeTimeout(Duration.ZERO)
                .build();

        httpClient = HttpClient.create(provider)
                .compress(true)
                .keepAlive(true)
                .protocol(HttpProtocol.HTTP11);

        //如果没有传入Scheduler 则使用默认的scheduler
        if (downloadScheduler == null) {

            //用于多线程下载文件的download pool
            ThreadPoolExecutor downloadPool = ThreadPoolUtil.newBuilder()
                    .poolName("HTTP_FILE_DOWNLOAD")
                    .coreThreads(10)
                    .enableMetric(true)
                    .maximumThreads(20)
                    .keepAliveSeconds(3600L)
                    //ring-buffer
                    .workQueue(new ArrayBlockingQueue<>(2000))
                    .threadFactory(new NamedThreadFactory("file-download-"))
                    .rejectedHandler(new ThreadPoolExecutor.CallerRunsPolicy())
                    .build();


            this.downloadScheduler = Schedulers.fromExecutorService(downloadPool);
        }

    }


    /**
     * 下载文件 ,并且返回下载之后的文件路径
     *
     * @param fileUrl 文件Url
     */
    public static CompletableFuture<Path> download(String fileUrl) throws Exception {
        return getInstance().downloadFile(fileUrl).toFuture();
    }


    /**
     * 用于下载文件
     *
     * @param fileUrl 文件的url
     */
    public Mono<Path> downloadFile(String fileUrl) {

        CloseTasks closeTasks = new CloseTasks();

        LocalDateTime startTime = LocalDateTime.now();
        try {
            //获取文件后缀
            String extension = StringUtils.getFilenameExtension(fileUrl);
            String fileName = UUID.randomUUID().toString().replace("-", "");
            Path filePath = Paths.get(fileName + "." + extension);

            //通过临时目录拼接对应的临时文件路径
            Path path = shareTempDir.getTempDirectory().resolveSibling(filePath);
            FileChannel fileChannel = FileChannel.open(path, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
            closeTasks.addTask(fileChannel::close, "Close http upload file channel");


            //多线程异常收集器,用于在多线程情况下的异常
            ExceptionManager manager = new ExceptionManager();
            Context downloadContext = Context.of(FileChannel.class, fileChannel, ExceptionManager.class, manager)
                    .put(DOWNLOAD_PATH, path);

            return httpClient
                    .headers(this::addHeader)
                    .head()
                    .uri(fileUrl)
                    .response()
                    .map(HttpClientResponse::responseHeaders)
                    .filter(this::validateHeaders)
                    .flatMap(headers -> parallelDownload(headers, downloadContext, fileUrl))
                    //如果不支持ranges，则通过普通的下载方式进行下载
                    .switchIfEmpty(Mono.defer(() -> this.downloadUniversalFile(fileUrl, downloadContext)))
                    .doOnSuccess(v -> log.info("Download file success, file is: {}", path.getFileName()))
                    .flatMap(ex -> throwIfHasError(manager, path))
                    .doOnError(ex -> deleteFileOnError(path))
                    .log()
                    .doFinally(signalType -> closeTasks.close())
                    .contextWrite(context -> context.putAll(downloadContext))
                    .doFinally(signalType -> log.info("Download file {} time is: {}", fileUrl, Duration.between(startTime, LocalDateTime.now())))
                    .thenReturn(path);
        } catch (IOException e) {
            //当执行出错时，关闭对应的io任务
            closeTasks.close();
            //返回异常
            return Mono.error(e);
        }
    }


    /**
     * 用于下载文件
     *
     * @param fileUrl 文件的url
     */
    public Mono<Path> singleDownloadFile(String fileUrl) {

        CloseTasks closeTasks = new CloseTasks();

        LocalDateTime startTime = LocalDateTime.now();
        try {
            //获取文件后缀
            String extension = StringUtils.getFilenameExtension(fileUrl);
            String fileName = UUID.randomUUID().toString().replace("-", "");
            Path filePath = Paths.get(fileName + "." + extension);

            //通过临时目录拼接对应的临时文件路径
            Path path = shareTempDir.getTempDirectory().resolveSibling(filePath);
            FileChannel fileChannel = FileChannel.open(path, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
            closeTasks.addTask(fileChannel::close, "Close http upload file channel");


            //多线程异常收集器,用于在多线程情况下的异常
            ExceptionManager manager = new ExceptionManager();
            Context downloadContext = Context.of(FileChannel.class, fileChannel)
                    .put(ExceptionManager.class, manager)
                    .put(DOWNLOAD_PATH, path);

            return downloadUniversalFile(fileUrl, downloadContext)
                    .doOnSuccess(v -> log.info("Download file success, file is: {}", path.getFileName()))
                    .flatMap(ex -> throwIfHasError(manager, path))
                    .doOnError(ex -> deleteFileOnError(path))
                    .log()
                    .doFinally(signalType -> closeTasks.close())
                    .contextWrite(context -> context.putAll(downloadContext))
                    .doFinally(signalType -> log.info("Download file {} time is: {}", fileUrl, Duration.between(startTime, LocalDateTime.now())))
                    .thenReturn(path);
        } catch (IOException e) {
            //当执行出错时，关闭对应的io任务
            closeTasks.close();
            //返回异常
            return Mono.error(e);
        }
    }

    /**
     * 判断当前文件是否需要拆分下载
     *
     * @param header 返回的http请求头
     */
    private boolean validateHeaders(io.netty.handler.codec.http.HttpHeaders header) {
        if (header.contains(HttpHeaders.ACCEPT_RANGES)) {
            int fileSize = header.getInt(HttpHeaders.CONTENT_LENGTH);
            return fileSize >= PARALLEL_FILE_SIZE;
        }
        return false;
    }

    /**
     * 下载普通文件
     *
     * @param fileUrl 文件的url地址
     */
    private Mono<String> downloadUniversalFile(String fileUrl, Context context) {
        log.info("downloadUniversalFile....");
        return httpClient
                .headers(this::addHeader)
                .get()
                .uri(fileUrl)
                .responseSingle((response, byteBufMono) -> {
                    //如果状态码错误，则直接返回异常
                    if (response.status().code() < 200 || response.status().code() >= 301) {
                        return Mono.error(new FileDownloadException("文件下载错误, 原因为:" + response.status()));
                    }

                    return byteBufMono;
                })
                .doOnNext(byteBuf -> writeBytes(context.get(FileChannel.class), byteBuf,
                        Range.create(0, byteBuf.writerIndex()), context.get(ExceptionManager.class)))
                .doOnNext(ReferenceCounted::release)
                .thenReturn(fileUrl);
    }

    /**
     * 多线程下载文件
     *
     * @param headers httpHeader
     * @param fileUrl 下载的文件url
     */
    private Mono<String> parallelDownload(io.netty.handler.codec.http.HttpHeaders headers, Context context, String fileUrl) {
        LocalDateTime startTime = LocalDateTime.now();
        return Flux.<Range>create(fluxSink -> splitRange(fluxSink, headers))
                //开启并行线程
                .parallel()
                //开启并行线程在执行结束后需要关闭,如果是web应用则不应关闭
                .runOn(downloadScheduler)
                .flatMap(range -> requestRangeBytes(range, context.get(FileChannel.class), context.get(ExceptionManager.class), fileUrl))
                //等待所有并行线程结束
                .sequential()
                .doFinally(signalType -> log.info("End time is: {}", Duration.between(startTime, LocalDateTime.now())))
                .then(Mono.just(fileUrl))
                //当多线程请求出错时，使用单线程的方式继续请求
                .onErrorResume(ex -> Mono.just(fileUrl));
    }


    private void addHeader(io.netty.handler.codec.http.HttpHeaders httpHeaders) {
        httpHeaders
//                .add(HttpHeaders.USER_AGENT, "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/108.0.0.0 Safari/537.36")
                .add(HttpHeaders.ACCEPT_ENCODING, "gzip,deflate");
    }

    /**
     * 如果执行错误, 把对应的临时文件进行删除
     *
     * @param filePath 临时文件路径
     */
    @SneakyThrows
    public void deleteFileOnError(Path filePath) {
        log.error("File download is error, delete file: {}", filePath);
        Files.deleteIfExists(filePath);
    }

    /**
     * 如果在执行中出现异常, 则抛出异常
     */
    private Mono<Void> throwIfHasError(ExceptionManager manager, Path path) {
        if (manager.hasError()) {
            //删除文件
            deleteFileOnError(path);
            return Mono.error(manager.getError());
        }

        return Mono.empty();
    }


    /**
     * 切割文件范围
     */
    private void splitRange(FluxSink<Range> fluxSink, io.netty.handler.codec.http.HttpHeaders httpHeaders) {
        Integer contentLength = httpHeaders.getInt(HttpHeaders.CONTENT_LENGTH);

        //每一份切分的文件大小
        int size = contentLength / splitCount;

        //文件切分的数量
        int count = splitCount;
        boolean mod = (contentLength % splitCount) == 0;
        count = mod ? count : ++count;

        for (int i = 0; i < count; i++) {
            //开始的位置
            int startPos = size * i;
            //结束的位置
            int endPos = size * (i + 1) - 1;
            //如果是最后一个范围并且无法整除，那么就说明还有最后一个范围
            if (i == count - 1 && !mod) {
                endPos = contentLength;
            }
            Range byteRange = Range.create(startPos, endPos);
            log.info("Download file range is: {}-{}", startPos, endPos);
            fluxSink.currentContext().put(HttpHeaders.CONTENT_LENGTH, contentLength);
            fluxSink.next(byteRange);
        }
        fluxSink.complete();
    }


    /**
     * 多线程请求不通的Range范围, 并且将请求完成的数据写入到FileChannel
     *
     * @param byteRange   文件的范围
     * @param fileChannel 文件需要写入的FileChannel
     * @param manager     异常管理器
     * @param fileUrl     文件Url
     */
    private Mono<Void> requestRangeBytes(Range byteRange, FileChannel fileChannel, ExceptionManager manager, String fileUrl) {
        //如果已经执行错误，那么就不在继续执行
        if (!manager.hasError()) {

            LocalDateTime startTime = LocalDateTime.now();

            return httpClient
                    .headers(header -> header.add(HttpHeaders.RANGE, byteRange.toString()))
                    .get()
                    .uri(fileUrl)
                    .responseContent()
                    .aggregate()
                    .doOnNext(byteBuf -> writeBytes(fileChannel, byteBuf, byteRange, manager))
//                    .doOnNext(byteBuf -> System.out.println(byteBuf.readableBytes()))
                    .doOnError(manager::addException)
                    .doFinally(signalType -> log.info("Request range {} time is: {}", byteRange, Duration.between(startTime, LocalDateTime.now())))
                    .then();
        }

        return Mono.empty();
    }


    private void writeBytes(FileChannel fileChannel, ByteBuf byteBuf, Range byteRange, ExceptionManager manager) {
        try {
            //如果已经有报错了，那么就不在继续写入文件
            if (!manager.hasError()) {
                log.info("Write byteBuf : {}, startPos: {}, endPos: {}", byteBuf, byteRange.startPos, byteRange.endPos);
                fileChannel.write(byteBuf.nioBuffer(), byteRange.startPos);
            }
        } catch (IOException ex) {
            log.error("Write file fail, cause of: ", ex);
            manager.addException(ex);
        }
    }


    /**
     * 关闭线程池
     */
    public void dispose() {
        downloadScheduler.dispose();
    }

    @AllArgsConstructor
    private static class Range {
        private static final String PREFIX = "bytes=";

        private final Integer startPos;
        private final Integer endPos;

        static Range create(int startPos) {
            return new Range(startPos, null);
        }

        static Range create(int startPos, int endPos) {
            return new Range(startPos, endPos);
        }

        static Range createForEnd(int endPos) {
            return new Range(null, endPos);
        }

        int writeBytes() {
            return endPos - startPos;
        }


        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append(PREFIX);
            builder.append(startPos);
            if (endPos != null) {
                builder.append("-").append(endPos);
            }

            return builder.toString();
        }
    }

    public static void main(String[] args) throws Exception {
        String imageUrl = "http://book-iot.oss-cn-beijing.aliyuncs.com/alpha/watermark/810b684c49ff4c418114be17da5fd293/cover.jpg";
        Path path = HttpFileClient.create(Schedulers.newSingle("download-file"))
                .downloadFile(imageUrl)
                .toFuture()
                .get(10, TimeUnit.MINUTES);

        HttpFileClient.getInstance().dispose();
        System.out.println(path);


    }

}
