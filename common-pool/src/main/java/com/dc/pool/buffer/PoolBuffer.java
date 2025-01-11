package com.dc.pool.buffer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;

public interface PoolBuffer {

    /**
     * close buffer and return buffer to pool
     */
    void close();


    /**
     * FileChannel#write(buffer,position) 底层采用pwrite实现，支持多线程写入
     *
     * @param fileChannel need write file channel
     * @param position    write position
     */
    void writeToChannel(FileChannel fileChannel, long position) throws IOException;

    /**
     * @param fileChannel need read file channel
     * @param position    position
     */
    void readFromChannel(FileChannel fileChannel, long position, long size) throws IOException;

    /**
     * read from mmap buffer
     *
     * @param fileChannel need read file channel
     * @param position    position
     */
    void readFromMmap(FileChannel fileChannel, long position, long size) throws IOException;

    /**
     * write sequence to pool buffer
     *
     * @param charSequence char sequence
     * @param charset      charset
     */
    void writeCharSequence(CharSequence charSequence, Charset charset);

    /**
     * write bytes
     *
     * @param bytes bytes
     */
    void writeBytes(byte[] bytes);

    /**
     * write byte buffer to pool buffer
     */
    void writeByteBuffer(ByteBuffer byteBuffer);

    /**
     * nio byte buffer
     */
    ByteBuffer nioBuffer();

    /**
     * 当前内存的长度
     */
    int size();
}
