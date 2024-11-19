package com.dc.tools.io.buffer;

import com.dc.tools.common.utils.Assert;
import com.dc.tools.common.utils.CloseTasks;
import com.google.common.collect.Sets;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.Unpooled;
import lombok.Getter;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * <p>异步读取文件流</p>
 * 也可以可以使用 {@link java.nio.channels.AsynchronousFileChannel}
 *
 * @author zy
 */
public class AsyncInputStream implements Closeable {

    /**
     * 文件对应的byteBuffer
     */
    private volatile ByteBuf byteBuf;

    private final CloseTasks closeTasks;

    private final CompletableFuture<Void> bufferFuture = new CompletableFuture<>();

    @Getter
    private final Executor executor;


    /**
     * @param filePath 文件路径
     */
    public AsyncInputStream(Path filePath, Executor executor) {
        this.closeTasks = new CloseTasks();
        this.executor = executor;
        //异步初始化当前文件内存
        this.executor.execute(() -> loadFileBuffer(filePath));
    }

    /**
     * @param filePath 文件路径
     */
    public AsyncInputStream(Path filePath, CloseTasks closeTasks, Executor executor) {
        Assert.isTrue(Files.exists(filePath), "The file {} is not exists, please check it", filePath);
        this.closeTasks = closeTasks;
        this.executor = executor;
        //异步初始化当前文件内存
        this.executor.execute(() -> loadFileBuffer(filePath));
    }

    /**
     * 需要被包装的inputStream
     *
     * @param inputStream 输入流
     */
    public AsyncInputStream(InputStream inputStream, Executor executor) {
        this.executor = executor;
        this.closeTasks = new CloseTasks();
        //异步初始化当前文件内存
        this.executor.execute(() -> loadInputStream(inputStream));
    }



    /**
     * 需要被包装的inputStream
     *
     * @param inputStream 输入流
     */
    public AsyncInputStream(byte[] bytes, Executor executor) {
        this.executor = executor;
        this.closeTasks = new CloseTasks();
        //异步初始化当前文件内存
        this.executor.execute(() -> loadInputStream(bytes));
    }

    /**
     * 需要被包装的inputStream
     *
     * @param buffer 输入流
     */
    public AsyncInputStream(ByteBuffer buffer, Executor executor) {
        this.executor = executor;
        this.closeTasks = new CloseTasks();
        //异步初始化当前文件内存
        this.executor.execute(() -> loadInputStream(buffer));
    }

    private void loadInputStream(ByteBuffer buffer) {
        try {
            this.byteBuf = Unpooled.wrappedBuffer(buffer);
            bufferFuture.complete(null);
        } catch (Exception ex) {
            bufferFuture.completeExceptionally(ex);
            closeTasks.close();
        }
    }

    private void loadInputStream(byte[] bytes) {
        try {
            this.byteBuf = Unpooled.wrappedBuffer(bytes);
            bufferFuture.complete(null);
        } catch (Exception ex) {
            bufferFuture.completeExceptionally(ex);
            closeTasks.close();
        }
    }

    private void loadInputStream(InputStream inputStream) {
        try {
            ByteBuf byteBuf = Unpooled.buffer(inputStream.available());
            byteBuf.writeBytes(inputStream, inputStream.available());
            this.byteBuf = byteBuf;
            bufferFuture.complete(null);
        } catch (Exception ex) {
            bufferFuture.completeExceptionally(ex);
            closeTasks.close();
        }
    }

    private void loadFileBuffer(Path filePath) {
        try (CloseTasks readTasks = new CloseTasks()) {
            //对于InputStream来说只需要关注文件读取即可
            //创建FileChannel时需要关注当前Path 是隶属于哪一个FileSystem
            FileChannel fileChannel = filePath.getFileSystem().provider().newFileChannel(filePath, Sets.newHashSet(StandardOpenOption.READ));
            readTasks.addTask(fileChannel::close, "Close file channel. file is: {}", filePath);
            //申请一块堆外内存
            //TODO: 待优化，对于部分场景可能需要读取inputStream 内部的数据,那么就会涉及到从堆外到堆内的复制
//            ByteBuf byteBuf = Unpooled.directBuffer((int) fileChannel.size());
            ByteBuf byteBuf = Unpooled.buffer((int) fileChannel.size());
            //将数据读取到ByteBuf中
            byteBuf.writeBytes(fileChannel, 0L, (int) fileChannel.size());
            //将数据加载到内存中
            this.byteBuf = byteBuf;
            closeTasks.addTask(() -> byteBuf.release(), "Close file direct buffer, file name is: {}", filePath);
            bufferFuture.complete(null);
        } catch (IOException e) {
            bufferFuture.completeExceptionally(e);
            //当出现错误时，直接关闭当前所有的任务，不管外部是否已经关闭CloseTask，CloseTask 内部已经实现幂等
            closeTasks.close();
        }


    }

    /**
     * 获取文件对应的数据流
     */
    public CompletableFuture<InputStream> getInputStream() {
        //这里需要对ByteBuf进行包装,因为要尽可能保证对于同一个数据流使用多次,而非单次
        return bufferFuture.thenApplyAsync(unused -> wrapInputStream(), executor);
    }

    /**
     * 获取原生的buffer数据
     */
    public CompletableFuture<ByteBuf> getBuffer() {
        //这里需要对ByteBuf进行包装,因为要尽可能保证对于同一个数据流使用多次,而非单次
        return bufferFuture.thenApplyAsync(unused -> byteBuf.slice(), executor);
    }

    /**
     * 将byteBuffer 包装成InputStream
     *
     * @param buffer buffer 数据
     */
    private ByteBufInputStream wrapInputStream() {
        ByteBuf sliceBuf = byteBuf.slice();
        return new ByteBufInputStream(sliceBuf);
    }


    /**
     * @return 同步获取文件对应的数据流
     */
    public InputStream getInputStreamSync() {
        //join 方法不会抛出检查异常，而是直接抛出运行时异常
        bufferFuture.join();
        return wrapInputStream();
    }


    @Override
    public void close() throws IOException {
        closeTasks.close();
    }
}
