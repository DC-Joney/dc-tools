package com.dc.pool.buffer;

import io.netty.buffer.ByteBuf;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@RequiredArgsConstructor
@Getter
@Setter
public class NettyPooledBuf {
    private final ByteBuf byteBuf;

    private final int size;


    public void writeChannel(){

    }



}
