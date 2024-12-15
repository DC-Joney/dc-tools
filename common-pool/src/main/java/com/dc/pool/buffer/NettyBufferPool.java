package com.dc.pool.buffer;

import cn.hutool.core.util.StrUtil;
import com.dc.tools.common.utils.SystemClock;
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
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;

@Slf4j
public class NettyBufferPool implements BufferPool<NettyBufferPool.PoolBuffer> {

    /**
     * 可使用的最大内存
     */
    private final long maxMemory;

    //默认最大可使用内存为16M
    private static final long DEFAULT_MAX_MEMORY = 16 << 20;

    /**
     * 已经使用的内存
     */
    private volatile AtomicLong usedState = new AtomicLong();

    /**
     * memory allocator
     */
    private final ByteBufAllocator allocator;

    /**
     * 线程等待队列
     */
    private final Deque<Thread> waitQueue = new ConcurrentLinkedDeque<>();

    private final AtomicBoolean closeState = new AtomicBoolean();

    public NettyBufferPool(ByteBufAllocator allocator, long maxMemory) {
        this.allocator = allocator;
        this.maxMemory = maxMemory;
    }

    public NettyBufferPool(ByteBufAllocator allocator) {
        this(allocator, DEFAULT_MAX_MEMORY);
    }


    @Override
    public NettyBufferPool.PoolBuffer allocate(int size, long maxTimeToBlock, TimeUnit timeUnit) throws InterruptedException {

        if (closeState.get()) {
            throw new AllocateBufferTimeoutException("memory pool is closed");
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

            //当前时间
            long nowTime = SystemClock.now();

            long state = usedState.get();
            long usedMemory = state >>> 1;

            boolean notify = false;

            if (currentWait >= maxTimeToBlock) {
                throw new AllocateBufferTimeoutException("allocate buffer is time out");
            }

            //如果内存可以分配则直接返回
            if (usedMemory + size <= maxMemory) {
                System.out.println(StrUtil.format("current memory: {}，size: {} maxMemory: {}", usedMemory, size, maxMemory));
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
            LockSupport.park(waitNanos - currentWait);
            System.out.println(StrUtil.format("park notify time: {}", Thread.currentThread()));
            //计算已经等待的时间
            currentWait += SystemClock.now() - nowTime;
        }

        ByteBuf buffer = allocator.buffer(size);
        return new PoolBuffer(buffer, size);
    }

    private synchronized void notifyThread() {
        Thread waitThread = null;
        while (waitQueue.peek() != null) {
            Thread pollThread = waitQueue.poll();
            //如果线程已经不存活了或者结束了
            if (!pollThread.isAlive()) {
                continue;
            }

            waitThread = pollThread;
            break;
        }

        //如果线程是存活的则唤醒
        if (waitThread != null) {
            LockSupport.unpark(waitThread);
        }
    }

    @Override
    public void deallocate(NettyBufferPool.PoolBuffer buffer) {
        ReferenceCountUtil.safeRelease(buffer);
        for (; ; ) {
            long state = usedState.get();
            if ((state & 1) == 0 && usedState.compareAndSet(state, state | 1)) {
                state -= (long) buffer.size << 1;
                if (state < 0)
                    state = 0;

                usedState.set(state >> 1 << 1);
                break;
            }


            Thread.yield();
        }

        notifyThread();
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

    @Getter
    @Setter
    @ToString
    @AllArgsConstructor
    public static class PoolBuffer {

        private ByteBuf byteBuf;

        private int size;
    }

    public static void main(String[] args) {
        PooledByteBufAllocator allocator = PooledByteBufAllocator.DEFAULT;
        NettyBufferPool bufferPool = new NettyBufferPool(allocator, 3 << 10);
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        for (int i = 0; i < 10; i++) {
            executorService.execute(() -> {
                try {
                    System.out.println("11111");
                    System.out.println(bufferPool.unallocatedMemory());
                    PoolBuffer allocate = bufferPool.allocate(512, 2000, TimeUnit.MILLISECONDS);
                    System.out.println(StrUtil.format("thread: {}, allocate: {}", Thread.currentThread(), allocate));
                    TimeUnit.MILLISECONDS.sleep(1500);
                    bufferPool.deallocate(allocate);
                } catch (Exception e) {
                    System.err.println(e.getMessage());
                    log.error("exception cause is: ", e);
                }

            });
        }
    }
}
