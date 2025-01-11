package com.dc.tools.common.utils;

import io.netty.util.internal.PlatformDependent;

import java.nio.ByteBuffer;

/**
 * 用于关闭对外存储
 *
 * @author zhangyang
 */
public class DirectBufferUtils {

    @Deprecated
    public static void release(ByteBuffer byteBuffer) {
        safeRelease(byteBuffer);
    }

    /**
     * 释放内存
     * @param byteBuffer 需要被释放的内存
     */
    public static void safeRelease(ByteBuffer byteBuffer) {
        if (byteBuffer != null) {
            try {
                PlatformDependent.freeDirectBuffer(byteBuffer);
            }catch (Exception ex) {
                PlatformDependent.freeDirectNoCleaner(byteBuffer);
            }
        }

    }

}
