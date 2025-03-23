package example;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.incubator.codec.http3.Http3HeadersFrame;
import io.netty.incubator.codec.http3.Http3DataFrame;
import io.netty.incubator.codec.http3.Http3Frame;

public class Http3ServerHandler implements ChannelHandler {

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        // Handler added 
    }

    @Override 
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        // Handler removed
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof Http3HeadersFrame) {
            // Handle headers frame
        } else if (msg instanceof Http3DataFrame) {
            // Handle data frame
        }

        ctx.fireChannelRead(msg);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}