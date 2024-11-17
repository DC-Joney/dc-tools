package com.dc.tools.common.utils;

import lombok.experimental.UtilityClass;

@UtilityClass
public class Power2 {

    /**
     * 计算x接近的 2的^n次方
     *
     * @param x 需要计算的值
     */
    public int power2(int x) {
        int leadingZeros = Integer.numberOfLeadingZeros(x - 1);
        return (-1 >>> leadingZeros) + 1;
    }

    public int power2ByLeftShift(int x) {
        int leadingZeros = Integer.numberOfLeadingZeros(x - 1);
        return 1 << (32 - leadingZeros);
    }

    /**
     * 计算x向上附近的 multiplier的倍数
     *
     * @param x          计算x向上取整的倍数值
     * @param multiplier 倍数，倍数必须为 2的指数
     */
    public int computeMultiple(int x, int multiplier) {
        int power2 = power2(multiplier);
        int r = multiplier & power2;
        int h = multiplier & (power2 - 1);
        if (r != 0 && h != 0) {
            throw new IllegalArgumentException("multiplier must be a power of 2");
        }

        int mask = power2 - 1;
        return (x & mask) == 0 ? x : (x & ~mask) + power2;
    }
}
