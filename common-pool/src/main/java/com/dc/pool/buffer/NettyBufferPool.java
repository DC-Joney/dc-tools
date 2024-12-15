package com.dc.pool.buffer;

import cn.hutool.core.util.StrUtil;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.util.ReferenceCountUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;

/**
 * Netty 内存池实现，支持控制内存大小
 *
 * @author zy
 * @apiNote 控制的内存并不准确，因为是采用netty buffer pool实现的，netty会自动对齐内存
 */
@Slf4j
public class NettyBufferPool implements BufferPool<NettyBufferPool.PoolBuffer> {

    /**
     * 默认最大可使用内存为16M
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


    public NettyBufferPool(ByteBufAllocator allocator, long maxMemory) {
        this.allocator = allocator;
        this.maxMemory = maxMemory;
    }

    public NettyBufferPool(ByteBufAllocator allocator) {
        this(allocator, DEFAULT_MAX_MEMORY);
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
    public NettyBufferPool.PoolBuffer allocate(int size, long maxTimeToBlock, TimeUnit timeUnit) throws InterruptedException {
        //如果内存池已经是关闭状态则直接抛出异常
        if (closeState.get()) {
            throw new BufferPoolException("memory pool is closed, please create new pool");
        }

        try {
            //最大等待的时间
            long waitNanos = timeUnit.toNanos(maxTimeToBlock);
            //如果等待的时间<0则表示无限等待直到有可以使用
            if (maxTimeToBlock < 0) {
                waitNanos = Long.MAX_VALUE;
            }

            //已经等待的时间
            long currentWait = 0;

            for (; ; ) {
                //获取当前的时间
                long nowTime = System.nanoTime();
                //获取当前的状态
                long state = usedState.get();
                //获取当前使用的内存
                long usedMemory = state >>> 1;

                if (currentWait >= waitNanos) {
                    throw new AllocateBufferTimeoutException("allocate buffer is time out, time is: {}, waitTime is: {}", currentWait / 1000000, waitNanos / 1000000);
                }

                //如果内存可以分配则直接返回
                if (usedMemory + size <= maxMemory) {
                    log.debug("free memory: {}，size: {} maxMemory: {}", (maxMemory - usedMemory), size, maxMemory);
                    if ((state & 1) == 0 && usedState.compareAndSet(state, state | 1)) {
                        //计算新的size
                        state += ((long) size << 1);

                        //如果获取到自旋锁的线程在获取完成内存后依旧有内存可使用，则唤醒下一个线程继续获取
                        if (state >>> 1 < maxMemory) {
                            notifyThread();
                        }

                        //释放自旋锁
                        usedState.set(state >>> 1 << 1);
                        break;
                    }

                    Thread.yield();
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
                log.info("park notify thread: {}, time is: {}", Thread.currentThread(), currentWait / 1000000);
            }

            //通过netty的allocator分配内存
            ByteBuf buffer = allocator.buffer(size);
            return new PoolBuffer(buffer, size);
        } catch (Exception e) {
            //如果是中断异常则直接抛出
            Throwables.throwIfInstanceOf(e, InterruptedException.class);
            //如果是内存池异常则直接抛出
            Throwables.throwIfInstanceOf(e, BufferPoolException.class);
            //否则将异常包装为
            throw new BufferPoolException(e, "allocate size {} from memory pool error", size);
        }

    }

    /**
     * 唤醒队列中的线程
     */
    private void notifyThread() {
        Thread waitThread = null;
        List<Thread> noAliveThreads = Lists.newArrayList();
        while ((waitThread = waitQueue.poll()) != null) {
            //如果线程已经不存活了或者结束了则直接寻找下一个
            //TODO: 阻塞中的线程是否会存在假死现象
            if (!waitThread.isAlive()) {
                noAliveThreads.add(waitThread);
                continue;
            }

            break;
        }

        //如果线程出现假死则一起唤醒
        noAliveThreads.forEach(LockSupport::unpark);

        //如果有阻塞的线程或者是存活的则唤醒
        if (waitThread != null) {
            LockSupport.unpark(waitThread);
        }
    }

    /**
     * 向内存池归还内存
     *
     * @param buffer 从内存池中获取的内存
     */
    @Override
    public void deallocate(NettyBufferPool.PoolBuffer buffer) {
        //释放当前内存
        ReferenceCountUtil.safeRelease(buffer);
        for (; ; ) {
            //获取当前状态
            long state = usedState.get();
            //添加自旋锁
            if ((state & 1) == 0 && usedState.compareAndSet(state, state | 1)) {
                state -= (long) buffer.size << 1;
                if (state < 0)
                    state = 0;

                //设置释放后的内存大小
                usedState.set(state >> 1 << 1);
                //唤醒后续线程
                notifyThread();
                break;
            }


            Thread.yield();
        }


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
        closeState.compareAndSet(false, true);
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
        notifyThread();
    }

    /**
     * Pool buffer for allocate
     */
    @Getter
    @Setter
    @ToString
    @AllArgsConstructor
    public static class PoolBuffer {

        /**
         * 分配的内存
         */
        private ByteBuf byteBuf;

        /**
         * 分配的内存大小，因为netty会自动对其内存，所以需要记录分配的内存大小
         */
        private int size;
    }

    public static void main(String[] args) throws InterruptedException {
        PooledByteBufAllocator allocator = PooledByteBufAllocator.DEFAULT;
        NettyBufferPool bufferPool = new NettyBufferPool(allocator, 2000);
        ExecutorService executorService = Executors.newFixedThreadPool(4);
        for (int i = 0; i < 4; i++) {
            executorService.execute(() -> {
                try {
                    PoolBuffer allocate = bufferPool.allocate(1000, 2000, TimeUnit.MILLISECONDS);
                    System.out.println(StrUtil.format("thread: {}, allocate: {}", Thread.currentThread(), allocate));
                    TimeUnit.MILLISECONDS.sleep(1500);
                    bufferPool.deallocate(allocate);
                } catch (Exception e) {
                    e.printStackTrace();
                }

            });
        }

    }
}
