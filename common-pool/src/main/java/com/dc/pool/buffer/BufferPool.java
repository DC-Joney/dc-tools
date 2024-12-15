package com.dc.pool.buffer;

import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

/**
 * Buffer pool for memory
 *
 * @author zhangyang
 */
public interface BufferPool<BUF> {

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

}
