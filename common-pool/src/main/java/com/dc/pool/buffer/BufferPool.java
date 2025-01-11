package com.dc.pool.buffer;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.concurrent.TimeUnit;

/**
 * Buffer pool for memory
 * <p>
 * 在线程数越多的情况下，比如32线程时，NettyBufferPool 与 NettyLockedBufferPool性能是持平的
 * 在低线程情况下，NettyBufferPool的性能是高于NettyLockedBufferPool的
 *
 * @author zhangyang
 * @see NettyBufferPool
 * @see NettyLockedBufferPool
 */
public interface BufferPool<BUF extends PoolBuffer> {

    /**
     * 从内存池中获取一块内存，如果内存池没有足够的内存就会一直阻塞直到有足够的内存或者是阻塞时间达到 {@code maxTimeToBlock}
     *
     * @param size           开辟的内存大小
     * @param maxTimeToBlock 当内存池没有足够的内存时需要等待的时间
     * @return 返回开辟的ByteBuffer
     */
    BUF allocate(int size, long maxTimeToBlock, TimeUnit timeUnit) throws InterruptedException;

    /**
     * 不建议使用这种方式，因为这会导致线程一直阻塞直到内存池有足够的内存
     *
     * @param size 开辟的内存大小
     * @return 返回开辟的ByteBuffer
     */
    @Deprecated
    default BUF allocate(int size) throws InterruptedException {
        return allocate(size, -1, TimeUnit.MILLISECONDS);
    }

    /**
     * @param data    需要包装为buffer的数据
     * @param charset charset
     * @return 返回包装好的内存数据
     */
    default BUF wrapCharSequence(String data, Charset charset, long maxTimeToBlock, TimeUnit timeUnit) throws InterruptedException {
        ByteBuffer byteBuffer = charset.encode(data);
        BUF allocate = allocate(byteBuffer.remaining(), maxTimeToBlock, timeUnit);
        try {
            allocate.writeByteBuffer(byteBuffer);
            return allocate;
        } catch (Exception e) {
            deallocate(allocate);
            throw e;
        }
    }


    /**
     * 将ByteBuffer 归回到内存池中
     *
     * @param buffer 从内存池中获取的内存
     */
    void deallocate(BUF buffer);

    /**
     * 内存池中未开辟的内存，既剩余的内存空间
     */
    long unallocatedMemory();

    /**
     * The total size of memory pool
     */
    long totalMemory();

    /**
     * Close buffer pool
     */
    void close();

    /**
     * 内存池名称
     */
    String name();

    /**
     * 当前等待的线程数量
     */
    int waitThreads();

}