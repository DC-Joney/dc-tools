package com.dc.pool.buffer;

import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import io.netty.util.TimerTask;
import lombok.extern.slf4j.Slf4j;

import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 不设定 poolSize大小的内存池申请，如果需要使用规整内存块请见 {@linkplain BufferChunkedPool}
 * <p>
 * 由于在特殊的场景中需要大量的消耗内存，这样会导致jvm频繁的gc<p>
 * 这里通过设置内存的阀值来保证可以减少gc的频率，并且由于不会在{@code BufferPool}中缓存对应的 {@code ByteBuffer}，所以在获取内存时会有部分的开销<p></p>
 * 如果对于内存使用有特定大小的情况下请使用 {@linkplain BufferChunkedPool}
 *
 * @author zhangyang
 * @apiNote Change from Kafka {@code BufferPool}
 * @see BufferPool
 */
@Slf4j
public class NoPaddingBufferPool implements BufferPool<ByteBuffer> {

    /**
     * 全部内存大小
     */
    private long totalMemory;


    private final Lock lock;

    /**
     * 用于在获取内存时如果内存池内存不足则添加到等待队列<p>
     * 主要是为了避免线程饥饿的问题
     */
    private final Deque<Condition> waiters;

    /**
     * 未开辟的可用内存，当free列表中没有可用内存时，就可以判断 nonPooledAvailableMemory 是否 > size，如果大于则通过allocateByteBuffer进行开启
     * ，总可用内存 =  nonPooledAvailableMemory + free * poolableSize
     */
    private long nonPooledAvailableMemory;

    private boolean closed;

    private static final HashedWheelTimer timer = new HashedWheelTimer();

    public NoPaddingBufferPool(long memory) {
        this.lock = new ReentrantLock();
        this.waiters = new ArrayDeque<>();
        this.totalMemory = memory;
        this.nonPooledAvailableMemory = memory;
        this.closed = false;
        init();
    }

    private void init() {
        timer.newTimeout(new StaticTimeTask(), 1, TimeUnit.SECONDS);
    }


    public class StaticTimeTask implements TimerTask {

        @Override
        public void run(Timeout timeout) throws Exception {
            //打印当前内存池的使用情况
            log.info("Buffer pool available size: {}, used size: {}, total size: {}", unallocatedMemory(),
                    totalMemory() - unallocatedMemory(), totalMemory());

            //每1分钟打印下当前内存池的使用情况
            timer.newTimeout(this, 1, TimeUnit.MINUTES);
        }
    }

    /**
     * Allocate a buffer of the given size. This method blocks if there is not enough memory and the buffer pool
     * is configured with blocking mode.
     *
     * @param size           The buffer size to allocate in bytes
     * @param maxTimeToBlock The maximum time in milliseconds to block for buffer memory to be available
     * @throws InterruptedException     If the thread is interrupted while blocked
     * @throws IllegalArgumentException if size is larger than the total memory controlled by the pool (and hence we would block
     *                                  forever)
     */
    public ByteBuffer allocate(int size, long maxTimeToBlock, TimeUnit timeUnit) throws InterruptedException {
        if (size > this.totalMemory)
            throw new IllegalArgumentException("Attempt to allocate " + size
                    + " bytes, but there is a hard limit of "
                    + this.totalMemory
                    + " on memory allocations.");

        this.lock.lock();

        if (this.closed) {
            this.lock.unlock();
            throw new BufferPoolAlreadyClosedException("Producer closed while allocating memory");
        }

        try {
            // now check if the request is immediately satisfiable with the
            // memory on hand or if we need to block
            //获取空闲的可用内存大小
            long freeSize = nonPooledAvailableMemory;

            //如果未开启的内存 + 空闲队列内存大小足够对当前的申请内存进行开启，则直接开辟
            if (this.nonPooledAvailableMemory >= size) {
                //未开辟的可用内存为 nonPooledAvailableMemory - size
                this.nonPooledAvailableMemory -= size;
            }

            //否则认为当前内存不足够进行分配
            else {
                // we are out of memory and will have to block
                //从当前内存池中拿到的内存
                int accumulated = 0;

                //创建一个新的条件队列
                Condition moreMemory = this.lock.newCondition();
                try {
                    //获取内存的最大阻塞时间
                    long remainingTimeToBlockNs = TimeUnit.NANOSECONDS.convert(maxTimeToBlock, timeUnit);
                    //如果 maxTimeToBlock 则永久等待直到内存池开辟出足够的空间
                    if (maxTimeToBlock == -1) {
                        remainingTimeToBlockNs = Long.MAX_VALUE;
                    }


                    this.waiters.addLast(moreMemory);
                    // loop over and over until we have a buffer or have reserved
                    // enough memory to allocate one
                    //循环等待从当前内存池到获取内存，直到获取到的内存可以足够满足当前的申请size
                    while (accumulated < size) {
                        long startWaitNs = System.nanoTime();
                        long timeNs;
                        boolean waitingTimeElapsed;
                        try {
                            waitingTimeElapsed = !moreMemory.await(remainingTimeToBlockNs, TimeUnit.NANOSECONDS);
                        } finally {
                            long endWaitNs = System.nanoTime();

                            //记录上一次获取内存的时候消耗的时间
                            timeNs = Math.max(0L, endWaitNs - startWaitNs);
                        }

                        if (this.closed)
                            throw new BufferPoolAlreadyClosedException("Producer closed while allocating memory");

                        //如果等待超时这里会抛出异常
                        if (waitingTimeElapsed) {
                            throw new AllocateBufferTimeoutException("Failed to allocate " + size + " bytes within the configured max blocking time "
                                    + TimeUnit.MILLISECONDS.convert(maxTimeToBlock, timeUnit) + " ms. " +
                                    "Total memory: " + totalMemory + " bytes. " +
                                    "Available memory: " + nonPooledAvailableMemory + " bytes.");
                        }

                        //剩余可等待的时间 = 将总时间 - 上一次内存消耗的时间
                        remainingTimeToBlockNs -= timeNs;

                        // we'll need to allocate memory, but we may only get
                        // part of what we need on this iteration
                        //判断nonPooledAvailableMemory是否已经满足当前牛才能块申请了
                        int got = (int) Math.min(size - accumulated, this.nonPooledAvailableMemory);

                        //当前未开辟内存 = nonPooledAvailableMemory - got(即将被分出去的内存)
                        this.nonPooledAvailableMemory -= got;

                        //将accumulated进行累加，到下一次循环时判断是否当前分配的内存是否已经可以满足当前内存块的申请
                        accumulated += got;
                    }

                    //如果程序正常运行完成需要将accumulated设置为0，因为这里要考虑到在异常的情况下需要回收没有开辟的内存
                    // Don't reclaim memory on throwable since nothing was thrown
                    accumulated = 0;
                } finally {

                    //正常情况下 accumulated = 0, 在异常情况下表示未被开辟出去的内存，需要进行回收
                    // When this loop was not able to successfully terminate don't loose available memory
                    this.nonPooledAvailableMemory += accumulated;
                    this.waiters.remove(moreMemory);
                }
            }
        } finally {
            // signal any additional waiters if there is more memory left
            // over for them
            try {
                //如果nonPooledAvailableMemory != 0 并且还有线程在进行等待获取内存则唤醒队列中的第一个等待者
                if (this.nonPooledAvailableMemory != 0 && !this.waiters.isEmpty()) {
                    this.waiters.peekFirst().signal();
                }
            } finally {
                // Another finally... otherwise find bugs complains
                lock.unlock();
            }
        }

        //只有在正常获取内存的情况下这里才会拿到内存
        return safeAllocateByteBuffer(size);
    }


    /**
     * Allocate a buffer.  If buffer allocation fails (e.g. because of OOM) then return the size count back to
     * available memory and signal the next waiter if it exists.
     */
    //分配缓冲区。如果缓冲区分配失败(例如因为 OOM) ，那么将大小计数返回到可用内存，并向下一个侍者发出信号(如果存在)。
    private ByteBuffer safeAllocateByteBuffer(long size) {
        boolean error = true;
        try {
            ByteBuffer buffer = ByteBuffer.allocate((int) size);
            error = false;
            return buffer;
        } finally {
            if (error) {
                this.lock.lock();
                try {
                    this.nonPooledAvailableMemory += size;
                    if (!this.waiters.isEmpty())
                        this.waiters.peekFirst().signal();
                } finally {
                    this.lock.unlock();
                }
            }
        }
    }

    /**
     * 将内存归还到给内存池
     *
     * @param buffer 从内存池中获取的内存
     * @param size   获取的内存大小
     */
    public void deallocate(ByteBuffer buffer, int size) {
        lock.lock();
        try {
            //将buffer中的数据清空,这里清空后可以强制外部无法使用
            buffer.clear();

            //将 availableMemory设置为 nonPooledAvailableMemory + size
            this.nonPooledAvailableMemory += size;

            //what is help gc ?
            buffer = null;

            //从等待队列中拿到第一个等待获取内存块的condition，然后释放，这样的作用在于避免现线程饥饿
            Condition moreMem = this.waiters.peekFirst();
            if (moreMem != null)
                moreMem.signal();
        } finally {
            lock.unlock();
        }
    }

    /**
     * 将内存归还到给内存池
     *
     * @param buffer 从内存池中获取的内存
     */
    public void deallocate(ByteBuffer buffer) {
        deallocate(buffer, buffer.capacity());
    }

    /**
     * Get the unallocated memory (not in the free list or in use)
     */
    public long unallocatedMemory() {
        lock.lock();
        try {
            return this.nonPooledAvailableMemory;
        } finally {
            lock.unlock();
        }
    }

    /**
     * The total memory managed by this pool
     */
    public long totalMemory() {
        return this.totalMemory;
    }


    public void close() {
        this.lock.lock();
        this.closed = true;
        try {
            for (Condition waiter : this.waiters)
                waiter.signal();
        } finally {
            this.lock.unlock();
        }
    }

}
