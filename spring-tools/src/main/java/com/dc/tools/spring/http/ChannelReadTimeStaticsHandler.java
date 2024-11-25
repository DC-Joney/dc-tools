package com.dc.tools.spring.http;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import lombok.extern.slf4j.Slf4j;

import java.net.SocketAddress;

/**
 * 统计写入耗时以及返回耗时
 *
 * @author zhangyang
 */
@Slf4j
@ChannelHandler.Sharable
class ChannelReadTimeStaticsHandler extends ChannelDuplexHandler {

    static final ChannelReadTimeStaticsHandler INSTANCE = new ChannelReadTimeStaticsHandler();


    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        SocketAddress socketAddress = ctx.channel().localAddress();
        log.info("Read data from local  address is: {}", socketAddress);
        super.channelReadComplete(ctx);
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        SocketAddress socketAddress = ctx.channel().localAddress();
        log.info("Write data to local address: {}, msgType is: {}", socketAddress, msg);
        super.write(ctx, msg, promise);
    }


}