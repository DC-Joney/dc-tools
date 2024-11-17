package com.dc.tools.io.buffer;

import com.dc.tools.common.utils.CloseTasks;
import com.dc.tools.common.utils.DirectBufferUtils;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * pdf source, 通过mmap 的方式来减少heap buffer的使用
 *
 * @author zhangyang
 */
@Slf4j
public class MappedBufferSource implements ByteBufferSource {

    /**
     * 用于异步通知的future
     */
    protected final CompletableFuture<MappedByteBuffer> bufferFuture = new CompletableFuture<>();

    /**
     * 异步加载文件的线程池
     */
    private final Executor executor;

    private final AtomicBoolean CLOSED = new AtomicBoolean();

    private final CloseTasks closeTasks;

    public MappedBufferSource(Path filePath) {
        this(filePath, new CloseTasks(), ForkJoinPool.commonPool());
    }

    public MappedBufferSource(Path filePath,CloseTasks closeTasks) {
        this(filePath, closeTasks, ForkJoinPool.commonPool());
    }

    public MappedBufferSource(Path filePath,CloseTasks closeTasks, Executor executor) {
        this.executor = executor;
        this.closeTasks = closeTasks;
        readBuffer(filePath);
    }

    private void readBuffer(Path path) {
        CloseTasks closeTasks = new CloseTasks();
        try {
            FileChannel fileChannel = FileChannel.open(path, StandardOpenOption.READ);
            closeTasks.addTask(fileChannel::close, "Close read mapped buffer from path");
            //读取文件数据
            readBuffer(fileChannel, closeTasks);
        } catch (IOException e) {
            //当出现异常时先关闭对应的任务
            closeTasks.close();
            bufferFuture.completeExceptionally(new ReadFileException("Read pdf buffer from {} error", e, path));
        }
    }

    private void readBuffer(FileChannel fileChannel, CloseTasks tasks) {
        executor.execute(() -> {
            closeTasks.addTasks(tasks);
            try {
                closeTasks.addTask(fileChannel::close, "Close mapped fileChannel");
                MappedByteBuffer buffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, fileChannel.size());
                closeTasks.addTask(() -> DirectBufferUtils.release(buffer), "Close read mapped buffer from channel");
                bufferFuture.complete(buffer);
            } catch (IOException e) {
                closeTasks.close();
                bufferFuture.completeExceptionally(new ReadFileException("Read pdf buffer from channel error", e));
            }
        });
    }

    @Override
    public Mono<ByteBuffer> getBuffer() {
        return Mono.fromCompletionStage(bufferFuture)
                .cast(ByteBuffer.class)
                .doOnNext(Buffer::rewind)
                .doOnError(ex -> release());
    }

    @Override
    public void release() {
        bufferFuture.whenComplete((byteBuffer, ex) -> {
            //保证只会被关闭一次
            if (CLOSED.compareAndSet(false, true)) {
                closeTasks.close();
            }
        });
    }




}
