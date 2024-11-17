package com.dc.tools.common;

import cn.hutool.core.date.SystemClock;
import com.dc.tools.common.utils.Assert;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 基于随机数生成唯一性ID
 *
 * @author zy
 */
public class SequenceIdGenerator implements IdGenerator {

    /**
     * 开始的时间
     */
    private static final long START_TIME = 1704038400000L;

    /**
     * MAGIC
     */
    private static final int MAX_COUNTER = ~(-1 << 20) << 1;

    /**
     * 支持的最大的时间
     */
    private static final long MAX_TIME = ~(-1L << 40);

    private final AtomicInteger counter = new AtomicInteger();

    private volatile long lastUpdateTime;

    @Override
    public long nextId() {
        long currentTime = SystemClock.now() - START_TIME;
        Assert.isTrue(currentTime < MAX_TIME, "time overflow max time");
        int randomValue = ThreadLocalRandom.current().nextInt(Integer.MAX_VALUE);

        //计算低bit的个数
        int randomLowerBits = 0;
        for (int i = 0; i < 7; i++) {
            if (((randomValue >> (i + 10)) & 1) == 0)
                randomLowerBits++;
        }

        //默认支持1ms 10W的并发，且低3bit受随机数影响
        int centerValue;
        for (; ; ) {
             centerValue = counter.get();
            if (centerValue <= MAX_COUNTER) {
                counter.getAndAdd(2);
                break;
            }

            if ((centerValue & 1) == 0 && counter.compareAndSet(centerValue, centerValue | 1)) {
                counter.set(0);
            }
        }

        lastUpdateTime = currentTime;
        //计算最终的数值
        return currentTime << 23 | centerValue >> 1 | randomLowerBits;
    }


    public int getCounter() {
        for (; ; ) {
            int id = counter.getAndAdd(2);
            if (id <= MAX_COUNTER) {
                return id;
            }

            if ((id & 1) == 0 && counter.compareAndSet(id, id | 1)) {
                counter.set(0);
            }
        }

    }

}
