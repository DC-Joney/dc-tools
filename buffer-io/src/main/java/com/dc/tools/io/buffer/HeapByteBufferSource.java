package com.dc.tools.io.buffer;

import com.dc.tools.common.utils.CloseTasks;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.ByteBuffer;
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
public class HeapByteBufferSource implements ByteBufferSource {

    /**
     * 用于异步通知的future
     */
    protected CompletableFuture<ByteBuffer> bufferFuture = new CompletableFuture<>();


    /**
     * 异步加载文件的线程池
     */
    private final Executor executor;

    private final AtomicBoolean CLOSED = new AtomicBoolean();

    private final CloseTasks closeTasks;

    public HeapByteBufferSource(Path filePath) {
        this(filePath, new CloseTasks(), ForkJoinPool.commonPool());
    }

    public HeapByteBufferSource(Path filePath, CloseTasks closeTasks) {
        this(filePath, closeTasks, ForkJoinPool.commonPool());
    }

    public HeapByteBufferSource(Path filePath, Executor executor) {
        this(filePath, new CloseTasks(), executor);
    }

    public HeapByteBufferSource(Path filePath, CloseTasks closeTasks, Executor executor) {
        this.executor = executor;
        this.closeTasks = closeTasks;
        readBuffer(filePath);
    }

    private void readBuffer(Path path) {
        try  {
            FileChannel fileChannel = FileChannel.open(path, StandardOpenOption.READ);
            closeTasks.addTask(fileChannel::close, "Close heap buffer channel from path: {}", path);
            //读取文件数据
            executor.execute(() -> {
                try {
                    ByteBuf byteBuf = Unpooled.buffer((int) fileChannel.size());
                    byteBuf.writeBytes(fileChannel, 0, (int) fileChannel.size());
                    ByteBuffer fileBuffer = ByteBuffer.allocate((int) fileChannel.size());
                    fileChannel.read(fileBuffer, 0);
                    bufferFuture.complete(fileBuffer);
                } catch (IOException e) {
                    closeTasks.close();
                    bufferFuture.completeExceptionally(new ReadFileException("Read heap buffer from {} error", e, path));
                }
            });
        } catch (IOException e) {
            //当出现异常时先关闭对应的任务
            closeTasks.close();
            bufferFuture.completeExceptionally(new ReadFileException("Read heap buffer from {} error", e, path));
        }
    }


    @Override
    public Mono<ByteBuffer> getBuffer() {
        return Mono.fromCompletionStage(bufferFuture)
                .map(ByteBuffer::slice)
                .cast(ByteBuffer.class)
                .doOnError(ex -> release());
    }

    public Mono<ByteBuf> getHeapBuf() {
        return Mono.fromCompletionStage(bufferFuture)
                .map(Unpooled::wrappedBuffer)
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
