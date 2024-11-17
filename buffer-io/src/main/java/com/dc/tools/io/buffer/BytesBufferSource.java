package com.dc.tools.io.buffer;

import com.dc.tools.common.utils.DirectBufferUtils;
import reactor.core.publisher.Mono;

import java.nio.ByteBuffer;

/**
 * @author zy
 */
public final class BytesBufferSource implements ByteBufferSource {

    private ByteBuffer byteBuffer;

    public BytesBufferSource(byte[] bytes) {
        this.byteBuffer = ByteBuffer.allocateDirect(bytes.length);
        this.byteBuffer.put(byteBuffer);
    }

    public BytesBufferSource(ByteBuffer buffer) {
        this.byteBuffer = buffer;
    }

    @Override
    public Mono<ByteBuffer> getBuffer() {
        return Mono.just(byteBuffer);
    }

    @Override
    public void release() {
        DirectBufferUtils.release(byteBuffer);
        byteBuffer = null;
    }
}
