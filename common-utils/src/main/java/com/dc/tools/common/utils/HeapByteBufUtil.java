package com.dc.tools.common.utils;


import lombok.experimental.UtilityClass;

/**
 * Utility class for heap buffers.
 *
 * Forked from <a href="https://github.com/netty/netty">Netty</a>.
 */
@UtilityClass
public final class HeapByteBufUtil {

    public static byte getByte(byte[] memory, int index) {
        return memory[index];
    }

    public static short getShort(byte[] memory, int index) {
        return (short) (memory[index] << 8 | memory[index + 1] & 0xFF);
    }

    public static short getShortLE(byte[] memory, int index) {
        return (short) (memory[index] & 0xff | memory[index + 1] << 8);
    }

    public static int getUnsignedMedium(byte[] memory, int index) {
        return (memory[index] & 0xff) << 16 | (memory[index + 1] & 0xff) << 8 | memory[index + 2] & 0xff;
    }

    public static int getUnsignedMediumLE(byte[] memory, int index) {
        return memory[index] & 0xff | (memory[index + 1] & 0xff) << 8 | (memory[index + 2] & 0xff) << 16;
    }

    public static int getInt(byte[] memory, int index) {
        return (memory[index] & 0xff) << 24 | (memory[index + 1] & 0xff) << 16 | (memory[index + 2] & 0xff) << 8
                | memory[index + 3] & 0xff;
    }

    public static int getIntLE(byte[] memory, int index) {
        return memory[index] & 0xff | (memory[index + 1] & 0xff) << 8 | (memory[index + 2] & 0xff) << 16
                | (memory[index + 3] & 0xff) << 24;
    }

    public static long getLong(byte[] memory, int index) {
        return ((long) memory[index] & 0xff) << 56 | ((long) memory[index + 1] & 0xff) << 48
                | ((long) memory[index + 2] & 0xff) << 40 | ((long) memory[index + 3] & 0xff) << 32
                | ((long) memory[index + 4] & 0xff) << 24 | ((long) memory[index + 5] & 0xff) << 16
                | ((long) memory[index + 6] & 0xff) << 8 | (long) memory[index + 7] & 0xff;
    }

    public static long getLongLE(byte[] memory, int index) {
        return (long) memory[index] & 0xff | ((long) memory[index + 1] & 0xff) << 8
                | ((long) memory[index + 2] & 0xff) << 16 | ((long) memory[index + 3] & 0xff) << 24
                | ((long) memory[index + 4] & 0xff) << 32 | ((long) memory[index + 5] & 0xff) << 40
                | ((long) memory[index + 6] & 0xff) << 48 | ((long) memory[index + 7] & 0xff) << 56;
    }

    public static void setByte(byte[] memory, int index, int value) {
        memory[index] = (byte) value;
    }

    public static void setShort(byte[] memory, int index, int value) {
        memory[index] = (byte) (value >>> 8);
        memory[index + 1] = (byte) value;
    }

    public static void setShortLE(byte[] memory, int index, int value) {
        memory[index] = (byte) value;
        memory[index + 1] = (byte) (value >>> 8);
    }

    public static void setMedium(byte[] memory, int index, int value) {
        memory[index] = (byte) (value >>> 16);
        memory[index + 1] = (byte) (value >>> 8);
        memory[index + 2] = (byte) value;
    }

    public static void setMediumLE(byte[] memory, int index, int value) {
        memory[index] = (byte) value;
        memory[index + 1] = (byte) (value >>> 8);
        memory[index + 2] = (byte) (value >>> 16);
    }

    public static void setInt(byte[] memory, int index, int value) {
        memory[index] = (byte) (value >>> 24);
        memory[index + 1] = (byte) (value >>> 16);
        memory[index + 2] = (byte) (value >>> 8);
        memory[index + 3] = (byte) value;
    }

    public static void setIntLE(byte[] memory, int index, int value) {
        memory[index] = (byte) value;
        memory[index + 1] = (byte) (value >>> 8);
        memory[index + 2] = (byte) (value >>> 16);
        memory[index + 3] = (byte) (value >>> 24);
    }

    public static void setLong(byte[] memory, int index, long value) {
        memory[index] = (byte) (value >>> 56);
        memory[index + 1] = (byte) (value >>> 48);
        memory[index + 2] = (byte) (value >>> 40);
        memory[index + 3] = (byte) (value >>> 32);
        memory[index + 4] = (byte) (value >>> 24);
        memory[index + 5] = (byte) (value >>> 16);
        memory[index + 6] = (byte) (value >>> 8);
        memory[index + 7] = (byte) value;
    }

    public static void setLongLE(byte[] memory, int index, long value) {
        memory[index] = (byte) value;
        memory[index + 1] = (byte) (value >>> 8);
        memory[index + 2] = (byte) (value >>> 16);
        memory[index + 3] = (byte) (value >>> 24);
        memory[index + 4] = (byte) (value >>> 32);
        memory[index + 5] = (byte) (value >>> 40);
        memory[index + 6] = (byte) (value >>> 48);
        memory[index + 7] = (byte) (value >>> 56);
    }


}