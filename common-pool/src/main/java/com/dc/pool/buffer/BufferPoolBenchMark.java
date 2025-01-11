package com.dc.pool.buffer;

import io.netty.buffer.PooledByteBufAllocator;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.concurrent.TimeUnit;

@Warmup(iterations = 3, time = 5)
@Measurement(iterations = 5, time = 5)
@State(Scope.Benchmark)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@BenchmarkMode({Mode.AverageTime, Mode.Throughput})
public class BufferPoolBenchMark {

    private BufferPool<NettyPoolBuf> bufferPool;

    private BufferPool<NettyPoolBuf> lockBufferPool;

    private final PooledByteBufAllocator allocator = PooledByteBufAllocator.DEFAULT;

    @Setup
    public void initialize() {
        bufferPool = new NettyBufferPool("1111", allocator,1 << 14);
        lockBufferPool = new NettyBufferPool("2222", allocator,1 << 14);
    }

    @Benchmark
    @Threads(64)
    @OperationsPerInvocation(10)
    public void bufferPool() {
        PoolBuffer poolBuffer = null;
        try {
            poolBuffer = bufferPool.allocate(1 << 10, 1, TimeUnit.SECONDS);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (poolBuffer != null) {
                poolBuffer.close();
            }
        }
    }


    @Benchmark
    @Threads(64)
    @OperationsPerInvocation(10)
    public void lockBufferPool() {
        PoolBuffer poolBuffer = null;
        try {
            poolBuffer = lockBufferPool.allocate(1 << 10, 1, TimeUnit.SECONDS);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (poolBuffer != null) {
                poolBuffer.close();
            }
        }
    }

    public static void main(String[] args) throws RunnerException {
        Options opts = new OptionsBuilder()
                // 表示包含的测试类
                .include(BufferPoolBenchMark.class.getSimpleName())
                .forks(1)
                .syncIterations(true)
                .build();

        new Runner(opts).run(); // 运行

    }

}
