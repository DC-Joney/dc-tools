package com.dc.pool.buffer;

import com.dc.tools.common.utils.Assert;
import com.dc.tools.common.utils.CloseTasks;
import io.netty.buffer.ByteBuf;
import io.netty.util.internal.PlatformDependent;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;

/**
 * NettyPoolBuf 用于表示从内存池中分配的内存
 *
 * @author zy
 */
@SuppressWarnings({"LombokGetterMayBeUsed"})
public class NettyPoolBuf implements PoolBuffer{

    /**
     * 分配的内存
     */
    private final ByteBuf byteBuf;

    /**
     * 分配的内存大小，因为netty会自动对其内存，所以需要记录分配的内存大小
     */
    private final int size;

    /**
     * 绑定的内存池
     */
    private final BufferPool<NettyPoolBuf> bufferPool;

    public NettyPoolBuf(ByteBuf byteBuf, int size, BufferPool<NettyPoolBuf> bufferPool) {
        this.byteBuf = byteBuf;
        this.size = size;
        this.bufferPool = bufferPool;
    }

    /**
     * FileChannel#write(buffer,position) 底层采用pwrite实现，支持多线程写入
     *
     * @param fileChannel need write file channel
     * @param position    write position
     */
    public void writeToChannel(FileChannel fileChannel, long position) throws IOException {
        byteBuf.readBytes(fileChannel, position, byteBuf.readableBytes());
    }

    /**
     * @param fileChannel need read file channel
     * @param position    position
     */
    public void readFromChannel(FileChannel fileChannel, long position, long size) throws IOException {
        byteBuf.writeBytes(fileChannel, position, (int) size);
    }

    /**
     * read from mmap buffer
     *
     * @param fileChannel need read file channel
     * @param position    position
     */
    public void readFromMmap(FileChannel fileChannel, long position, long size) throws IOException {
        Assert.isTrue(size < Integer.MAX_VALUE, "size must be greater than Integer.MAX_VALUE");
        // 创建一个直接字节缓冲区，容量等于文件大小
        try (CloseTasks closeTasks = new CloseTasks()) {
            MappedByteBuffer mappedByteBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, position, size);
            closeTasks.addTask(() -> PlatformDependent.freeDirectBuffer(mappedByteBuffer), "Close mmap buffer");
            // 映射文件通道到内存，然后复制到ByteBuf
            byteBuf.writeBytes(mappedByteBuffer);
        }
    }

    public void close() {
        bufferPool.deallocate(this);
    }

    public void writeCharSequence(CharSequence charSequence, Charset charset) {
        this.byteBuf.writeCharSequence(charSequence, charset);
    }

    public void writeBytes(byte[] bytes) {
        this.byteBuf.writeBytes(bytes);
    }

    public void writeByteBuffer(ByteBuffer byteBuffer) {
        this.byteBuf.writeBytes(byteBuffer);
    }

    public ByteBuffer nioBuffer() {
        return byteBuf.nioBuffer();
    }


    public int size() {
        return size;
    }

    public ByteBuf getByteBuf() {
        return byteBuf;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }

    public static NettyPoolBuf create(ByteBuf byteBuf, int size, BufferPool<NettyPoolBuf> bufferPool) {
        return new NettyPoolBuf(byteBuf, size, bufferPool);
    }
}
