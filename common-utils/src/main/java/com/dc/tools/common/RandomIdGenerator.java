package com.dc.tools.common;

import cn.hutool.core.date.SystemClock;
import com.dc.tools.common.utils.Assert;

import java.util.concurrent.ThreadLocalRandom;

/**
 * 基于随机数生成唯一性ID
 *
 * @author zy
 */
public class RandomIdGenerator implements IdGenerator {
    /**
     * 随机数的最大值
     */
    private static final int MAX_RANDOM_VALUE = ~(-1 << 30);

    /**
     * 随机数的最小值
     */
    private static final int MIN_RANDOM_VALUE = 1 << 10;

    /**
     * 开始的时间
     */
    private static final long START_TIME = 1704038400000L;

    /**
     * MAGIC
     */
    private static final int MAGIC = ~(-1 << 10);

    /**
     * 支持的最大的时间
     */
    private static final long MAX_TIME = ~(-1L << 40);

    @Override
    public long nextId() {
        long currentTime = SystemClock.now() - START_TIME;
        Assert.isTrue(currentTime < MAX_TIME, "time overflow max time");
        int randomValue = ThreadLocalRandom.current().nextInt(MIN_RANDOM_VALUE, MAX_RANDOM_VALUE);

        //计算低bit的个数
        int randomLowerBits = 0;
        for (int i = 0; i < 7; i++) {
            if (((randomValue >> (i)) & 1) == 0)
                randomLowerBits++;
        }

        int lowerValue = randomValue & MAGIC;
        int centerValue = randomValue >> 10 & MAGIC;
        int highValue = randomValue >> 20 & MAGIC;

        //计算随机数
        randomValue = centerValue << 10 | lowerValue ^ highValue;

        //计算最终的数值
        return currentTime << 23 | randomValue | randomLowerBits;
    }

}
