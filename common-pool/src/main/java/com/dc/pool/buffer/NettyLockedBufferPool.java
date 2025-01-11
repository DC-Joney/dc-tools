package com.dc.pool.buffer;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.util.ReferenceCountUtil;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 用于内存池申请内存, 底层采用Netty的内存池实现
 *
 * @author zy
 * @apiNote change from kafka, 通过 lock 实现，因为需要考虑到内存饥饿问题
 * @see NettyBufferPool
 */
public class NettyLockedBufferPool implements BufferPool<NettyPoolBuf> {

    private static final long DEFAULT_MAX_MEMORY = 16 << 20;

    /**
     * 内存池可分配的最大内存
     */
    private final long maxMemorySize;

    /**
     * 未分配的内存
     */
    private long availableMemory;

    /**
     * 等待队列
     */
    private final Deque<Condition> waitQueue = new ArrayDeque<>();

    /**
     * 用于申请内存的lock
     */
    private final Lock memoryLock = new ReentrantLock();

    /**
     * netty pool buffer allocator
     */
    private final ByteBufAllocator allocator;

    /**
     * pool close state
     */
    private boolean closed;

    /**
     * 内存池名称
     */
    private final String poolName;

    public NettyLockedBufferPool(String poolName, ByteBufAllocator allocator, long maxMemorySize) {
        this.maxMemorySize = maxMemorySize;
        this.allocator = allocator;
        this.availableMemory = maxMemorySize;
        this.poolName = poolName;
    }

    public NettyLockedBufferPool(String poolName, ByteBufAllocator allocator) {
        this(poolName, ByteBufAllocator.DEFAULT, DEFAULT_MAX_MEMORY);
    }

    @Override
    public NettyPoolBuf allocate(int size, long maxTimeToBlock, TimeUnit timeUnit) throws InterruptedException {

        if (size > maxMemorySize) {
            throw new BufferPoolException("The requested size is larger than the maximum size");
        }

        memoryLock.lock();

        //如果内存池已经关闭则直接释放锁
        if (closed) {
            //为什么要在锁内部支持close判断，是因为要考虑到close的并发安全
            memoryLock.unlock();
            throw new BufferPoolException("The memory pool is already closed");
        }

        try {

            //如果未开启的内存 + 空闲队列内存大小足够对当前的申请内存进行开启，则直接开辟
            if (this.availableMemory >= size) {
                //未开辟的可用内存为 nonPooledAvailableMemory - size
                this.availableMemory -= size;
            }

            //否则认为当前内存不足够进行分配
            else {
                //当前线程最大允许的等待时间
                long maxWaitTime = timeUnit.toNanos(maxTimeToBlock);
                if (maxTimeToBlock == -1) {
                    maxWaitTime = Long.MAX_VALUE;
                }

                //已经申请的内存大小
                long requestSize = 0;
                //等待时间
                long waitTime = 0;
                //当前等待的条件队列
                Condition condition = memoryLock.newCondition();

                try {
                    this.waitQueue.addLast(condition);

                    while (requestSize < size) {
                        //开始的时间
                        long startTime = System.nanoTime();

                        try {
                            long l = condition.awaitNanos(maxWaitTime - waitTime);
                        } finally {
                            long endTime = System.nanoTime();
                            //记录上一次获取内存的时候消耗的时间
                            waitTime += Math.max(0L, endTime - startTime);
                        }

                        if (this.closed)
                            throw new BufferPoolAlreadyClosedException("Producer closed while allocating memory");

                        //如果等待超时这里会抛出异常
                        if (waitTime >= maxWaitTime) {
                            throw new AllocateBufferTimeoutException("Failed to allocate " + size + " bytes within the configured max blocking time "
                                    + TimeUnit.MILLISECONDS.convert(maxTimeToBlock, timeUnit) + " ms. " +
                                    "Total memory: " + maxMemorySize + " bytes. " +
                                    "Available memory: " + availableMemory + " bytes.");
                        }


                        //判断nonPooledAvailableMemory是否已经满足当前牛才能块申请了
                        int got = (int) Math.min(size - requestSize, this.availableMemory);

                        //当前未开辟内存 = nonPooledAvailableMemory - got(即将被分出去的内存)
                        this.availableMemory -= got;

                        //将accumulated进行累加，到下一次循环时判断是否当前分配的内存是否已经可以满足当前内存块的申请
                        requestSize += got;

                    }

                    requestSize = 0;

                } finally {
                    //正常情况下 requestSize = 0, 在异常情况下表示未被开辟出去的内存，需要进行回收
                    this.availableMemory += requestSize;
                    this.waitQueue.remove(condition);
                }


            }
        } finally {
            try {
                //如果nonPooledAvailableMemory != 0 并且还有线程在进行等待获取内存则唤醒队列中的第一个等待者
                if (this.availableMemory != 0 && !this.waitQueue.isEmpty()) {
                    this.waitQueue.peekFirst().signal();
                }
            } finally {
                // Another finally... otherwise find bugs complains
                memoryLock.unlock();
            }

        }

        ByteBuf byteBuf = safeAllocateByteBuffer(size);
        return NettyPoolBuf.create(byteBuf, size, this);
    }

    //分配缓冲区。如果缓冲区分配失败(例如因为 OOM) ，那么将大小计数返回到可用内存，并向下一个侍者发出信号(如果存在)。
    private ByteBuf safeAllocateByteBuffer(long size) {
        boolean error = true;
        try {
            ByteBuf buffer = allocator.buffer((int) size);
            error = false;
            return buffer;
        } finally {
            if (error) {
                this.memoryLock.lock();
                try {
                    this.availableMemory += size;
                    if (!this.waitQueue.isEmpty())
                        this.waitQueue.peekFirst().signal();
                } finally {
                    this.memoryLock.unlock();
                }
            }
        }
    }

    @Override
    public void deallocate(NettyPoolBuf buffer) {
        memoryLock.lock();
        try {
            //将buffer中的数据清空,这里清空后可以强制外部无法使用
            ReferenceCountUtil.safeRelease(buffer.getByteBuf());

            //将 availableMemory设置为 nonPooledAvailableMemory + size
            this.availableMemory += buffer.size();

            //what is help gc ?
            buffer = null;

            //从等待队列中拿到第一个等待获取内存块的condition，然后释放，这样的作用在于避免现线程饥饿
            Condition moreMem = this.waitQueue.peekFirst();
            if (moreMem != null)
                moreMem.signal();
        } finally {
            memoryLock.unlock();
        }
    }

    @Override
    public long unallocatedMemory() {
        return availableMemory;
    }

    @Override
    public long totalMemory() {
        return maxMemorySize;
    }

    @Override
    public void close() {
        this.memoryLock.lock();
        this.closed = true;
        try {
            for (Condition waiter : this.waitQueue)
                waiter.signal();
        } finally {
            this.memoryLock.unlock();
        }
    }

    @Override
    public int waitThreads() {
        return waitQueue.size();
    }

    @Override
    public String name() {
        return poolName;
    }
}