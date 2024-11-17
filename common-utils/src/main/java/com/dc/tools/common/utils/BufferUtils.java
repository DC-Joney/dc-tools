package com.dc.tools.common.utils;


import java.nio.Buffer;

/**
 * Explicit cast to {@link Buffer} parent buffer type. It resolves issues with covariant return types in Java 9+ for
 * {@link java.nio.ByteBuffer} and {@link java.nio.CharBuffer}. Explicit casting resolves the NoSuchMethodErrors (e.g
 * java.lang.NoSuchMethodError: java.nio.ByteBuffer.flip(I)Ljava/nio/ByteBuffer) when the project is compiled with
 * newer Java version and run on Java 8.
 * <p/>
 * <a href="https://docs.oracle.com/javase/8/docs/api/java/nio/ByteBuffer.html">Java 8</a> doesn't provide override the
 * following Buffer methods in subclasses:
 *
 * <pre>
 * Buffer clear()
 * Buffer flip()
 * Buffer limit(int newLimit)
 * Buffer mark()
 * Buffer position(int newPosition)
 * Buffer reset()
 * Buffer rewind()
 * </pre>
 *
 * <a href="https://docs.oracle.com/javase/9/docs/api/java/nio/ByteBuffer.html">Java 9</a> introduces the overrides in
 * child classes (e.g the ByteBuffer), but the return type is the specialized one and not the abstract {@link Buffer}.
 * So the code compiled with newer Java is not working on Java 8 unless a workaround with explicit casting is used.
 *
 * Fork from Soft-JRaft
 */
public class BufferUtils {

    /**
     * @param buffer byteBuffer
     */
    public static void flip(Buffer buffer) {
        buffer.flip();
    }

    /**
     * @param buffer byteBuffer
     */
    public static void clear(Buffer buffer) {
        buffer.clear();
    }

    /**
     * @param buffer byteBuffer
     */
    public static void limit(Buffer buffer, int newLimit) {
        buffer.limit(newLimit);
    }

    /**
     * @param buffer byteBuffer
     */
    public static void mark(Buffer buffer) {
        buffer.mark();
    }

    /**
     * @param buffer byteBuffer
     */
    public static void position(Buffer buffer, int newPosition) {
        buffer.position(newPosition);
    }

    /**
     * @param buffer byteBuffer
     */
    public static void rewind(Buffer buffer) {
        buffer.rewind();
    }

    /**
     * @param buffer byteBuffer
     */
    public static void reset(Buffer buffer) {
        buffer.reset();
    }

}