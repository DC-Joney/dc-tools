package com.dc.pool.buffer;

import cn.hutool.core.date.SystemClock;
import com.dc.tools.common.utils.Assert;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.util.ReferenceCountUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;

/**
 * Netty 内存池实现，支持控制内存大小
 * <p>
 * Benchmark                    Mode  Cnt      Score      Error   Units
 * <p>
 * LockExample.bufferPool      thrpt    5  33807.989 ± 2595.237  ops/ms
 * <p>
 * LockExample.lockBufferPool  thrpt    5  34634.079 ± 5647.134  ops/ms
 * <p>
 * LockExample.bufferPool       avgt    5      0.001 ±    0.001   ms/op
 * <p>
 * LockExample.lockBufferPool   avgt    5      0.001 ±    0.001   ms/op
 * <p>
 *
 * @author zy
 * @apiNote 控制的内存并不准确，因为是采用netty buffer pool实现的，netty会自动对齐内存
 */
public class NettyBufferPool implements BufferPool<NettyPoolBuf> {

    private static final Logger log = LoggerFactory.getLogger(NettyBufferPool.class);

    /**
     * default memory pool size
     */
    private static final long DEFAULT_MAX_MEMORY = 16 << 20;

    /**
     * 可使用的最大内存
     */
    private final long maxMemory;

    /**
     * 已经使用的内存
     */
    private final AtomicLong usedState = new AtomicLong();

    /**
     * memory allocator
     */
    private final ByteBufAllocator allocator;

    /**
     * 线程等待队列
     */
    private final Deque<Thread> waitQueue = new ConcurrentLinkedDeque<>();

    /**
     * memory pool state
     */
    private final AtomicBoolean closeState = new AtomicBoolean();

    /**
     * 内存池名称
     */
    private final String poolName;

    /**
     * memory stats recorder
     */
    private final BufferPoolMetricsRecorder recorder;

    public NettyBufferPool(String poolName, ByteBufAllocator allocator, long maxMemory) {
        Assert.isTrue(maxMemory > 0, "maxMemory must be non-negative");
        this.allocator = allocator;
        this.maxMemory = maxMemory;
        this.poolName = poolName;
        this.recorder = new BufferPoolMetricsRecorder(poolName, this);
    }

    public NettyBufferPool(String poolName, ByteBufAllocator allocator) {
        this(poolName, allocator, DEFAULT_MAX_MEMORY);
    }


    /**
     * 从内存池中获取内存，当获取不到内存时会等待 maxTimeToBlock，如果还是无法获取则抛出异常
     *
     * @param size           开辟的内存大小
     * @param maxTimeToBlock 当内存池没有足够的内存时需要等待的时间
     * @param timeUnit       time unit
     * @return 返回开辟的内存
     * @throws InterruptedException 支持线程中断
     */
    @Override
    public NettyPoolBuf allocate(int size, long maxTimeToBlock, TimeUnit timeUnit) throws InterruptedException {

        recorder.requestInc();
        long startTime = SystemClock.now();

        try {

            //如果内存池已经是关闭状态则直接抛出异常
            if (closeState.get()) {
                recorder.failInc();
                throw new BufferPoolException("memory pool is closed, please create new pool");
            }

            if (size > maxMemory) {
                throw new BufferPoolException("allocated size is overflow max size");
            }

            //最大等待的时间
            long waitNanos = timeUnit.toNanos(maxTimeToBlock);
            //如果等待的时间<0则表示无限等待直到有可以使用
            if (maxTimeToBlock < 0) {
                waitNanos = Long.MAX_VALUE;
            }

            //已经等待的时间
            long currentWait = 0;


            for (; ; ) {
                //当被唤醒的线程从新请求内存时需要判断内存池是否已经关闭
                if (closeState.get()) {
                    throw new BufferPoolException("memory pool is closed");
                }
                //获取当前的时间
                long nowTime = System.nanoTime();
                //获取当前的状态
                long state = usedState.get();
                //获取当前使用的内存
                long usedMemory = state >>> 1;
                //如果当前的等待时间已经大于最大等待时间
                if (currentWait >= waitNanos) {
                    throw new AllocateBufferTimeoutException("allocate buffer is time out, time is: {}, waitTime is: {}", currentWait / 1000000, waitNanos / 1000000);
                }

                //如果内存可以分配则直接返回
                if (usedMemory + size <= maxMemory) {
//                    log.debug("free memory: {}，size: {} maxMemory: {}", (maxMemory - usedMemory), size, maxMemory);
                    if ((state & 1) == 0 && usedState.compareAndSet(state, state | 1)) {
                        try {
                            //当被唤醒的线程从新请求内存时需要判断内存池是否已经关闭
                            if (closeState.get()) {
                                throw new BufferPoolException("memory pool is closed");
                            }

                            //计算新的size
                            state += ((long) size << 1);
                        } finally {
                            //释放自旋锁,避免notifyThread异常而导致的自旋锁未释放
                            usedState.set(state >>> 1 << 1);

//                            //如果还有空余的内存则尝试唤醒下一个线程
//                            if (hasFreeMemory() && !waitQueue.isEmpty()) {
//                                notifyFirstThread();
//                            }
                        }

                        break;
                    }

                    //see: https://atomic.korins.ky/
                    LockSupport.parkNanos(1);
                    continue;
                }

                //将当前线程添加到队列
                waitQueue.offer(Thread.currentThread());
                //可休眠的时间
                LockSupport.parkNanos(waitNanos - currentWait);
                //支持线程中断，由线程中断引起的唤醒则会抛出异常，不会再进行等待
                if (Thread.currentThread().isInterrupted()) {
                    throw new InterruptedException("Memory pool wait thread is interrupt");
                }

                //计算已经等待的时间
                currentWait += System.nanoTime() - nowTime;
//                log.debug("park notify thread: {}, time is: {}", Thread.currentThread(), currentWait / 1000000);
            }

            //通过netty的allocator分配内存
            ByteBuf buffer = allocator.buffer(size);
            return NettyPoolBuf.create(buffer, size, this);
        } catch (Exception e) {
            recorder.failInc();
            //如果是中断异常则直接抛出
            Throwables.throwIfInstanceOf(e, InterruptedException.class);
            //如果是内存池异常则直接抛出
            Throwables.throwIfInstanceOf(e, BufferPoolException.class);
            //否则将异常包装为
            throw new BufferPoolException(e, "Allocate size {} from memory pool error", size);
        } finally {
            long executeTime = SystemClock.now() - startTime;
            recorder.recordWait(executeTime);
        }

    }


    /**
     * 唤醒队列中的线程
     */
    private void notifyThread(boolean notifyAll) {
        Thread waitThread = null;
        List<Thread> waitThreads = new ArrayList<>();
        List<Thread> noAliveThreads = Lists.newArrayList();
        //不可以先用peek再用poll会导致空指针，因为是并发获取的
        while ((waitThread = waitQueue.poll()) != null) {
            //如果线程已经不存活了或者结束了则直接寻找下一个
            //TODO: 阻塞中的线程是否会存在假死现象
            if (!waitThread.isAlive()) {
                noAliveThreads.add(waitThread);
                continue;
            }

            waitThreads.add(waitThread);
            if (!notifyAll) {
                break;
            }
        }

        //如果线程出现假死则一起唤醒
        noAliveThreads.forEach(Thread::interrupt);
        //如果有阻塞的线程或者是存活的则唤醒
        if (!waitThreads.isEmpty()) {
            waitThreads.forEach(LockSupport::unpark);
        }
    }

    /**
     * 向内存池归还内存
     *
     * @param buffer 从内存池中获取的内存
     */
    @Override
    public void deallocate(NettyPoolBuf buffer) {
        //释放当前内存
        ReferenceCountUtil.safeRelease(buffer.getByteBuf());
        for (; ; ) {
            //获取当前状态
            long state = usedState.get();
            //添加自旋锁
            if ((state & 1) == 0 && usedState.compareAndSet(state, state | 1)) {
                try {
                    state -= (long) buffer.size() << 1;
                    if (state < 0)
                        state = 0;

                    break;
                } finally {
                    //设置释放后的内存大小,这里保证了已经释放完成自旋锁了，不会有太大的影响波及
                    usedState.set(state >>> 1 << 1);

                    //唤醒后续线程
                    notifyFirstThread();
                }
            }

            //see: https://atomic.korins.ky/
            LockSupport.parkNanos(1);
        }

    }

    public boolean hasFreeMemory() {
        return usedState.get() >>> 1 < maxMemory;
    }

    @Override
    public long unallocatedMemory() {
        return maxMemory - (usedState.get() >>> 1);
    }

    @Override
    public long totalMemory() {
        return maxMemory;
    }

    @Override
    public void close() {
        if (closeState.compareAndSet(false, true)) {
            for (; ; ) {
                //获取当前状态
                long state = usedState.get();
                //添加自旋锁
                if ((state & 1) == 0 && usedState.compareAndSet(state, state | 1)) {
                    try {
                        //释放所有的线程，避免在close时有其他线程还在自旋中尝试获取内存，保证
                        notifyThread(true);
                    } finally {
                        //设置释放后的内存大小,这里保证了已经释放完成自旋锁了，不会有太大的影响波及
                        usedState.set(state >>> 1 << 1);
                    }

                    break;
                }

                //see: https://atomic.korins.ky/
                LockSupport.parkNanos(1);
            }
        }
    }

    /**
     * 正在等待的线程数量
     */
    public int waitThreads() {
        return waitQueue.size();
    }

    /**
     * 如果队列中存在阻塞的线程，则唤醒队列中的第一个线程
     */
    public void notifyFirstThread() {
        notifyThread(false);
    }

    @Override
    public String name() {
        return poolName;
    }
}