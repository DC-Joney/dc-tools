package com.dc.tools.io.buffer;

import reactor.core.publisher.Mono;

import java.nio.ByteBuffer;

/**
 * 用于获取Buffer数据
 *
 * @author zy
 */
public interface ByteBufferSource {

    /**
     * 获取Buffer对象
     */
    Mono<ByteBuffer> getBuffer();


    /**
     * 释放buffer对象
     */
    void release();

}
