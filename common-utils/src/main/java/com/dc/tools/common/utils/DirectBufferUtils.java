package com.dc.tools.common.utils;

import sun.misc.Cleaner;
import sun.nio.ch.DirectBuffer;

import java.nio.ByteBuffer;

/**
 * 用于关闭对外存储
 *
 * @author zhangyang
 */
public class DirectBufferUtils {

    public static void release(ByteBuffer byteBuffer) {
        //如果是堆外内存则将该部分内存释放
        if (byteBuffer instanceof DirectBuffer) {
            Cleaner cleaner = ((DirectBuffer) byteBuffer).cleaner();
            if (cleaner != null) {
                cleaner.clean();
            }
        }
    }

}
