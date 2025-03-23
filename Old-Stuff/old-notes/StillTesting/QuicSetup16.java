// Http3Server.java
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.incubator.codec.quic.QuicServerCodecBuilder;
import io.netty.incubator.codec.http3.Http3FrameCodec;

public class Http3Server {
    private final int port;

    public Http3Server(int port) {
        this.port = port;
    }

    public void start() throws Exception {
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
             .channel(QuicServerCodecBuilder.class)
             .childHandler(new Http3ServerInitializer());

            ChannelFuture f = b.bind(port).sync();
            f.channel().closeFuture().sync();
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }
}

// Http3ServerInitializer.java
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http2.Http2HeadersFrame;
import io.netty.incubator.codec.http3.Http3FrameCodec;

public class Http3ServerInitializer extends ChannelInitializer<SocketChannel> {
    @Override
    protected void initChannel(SocketChannel ch) {
        ChannelPipeline p = ch.pipeline();
        p.addLast(new Http3FrameCodec());
        p.addLast(new Http3FrameHandler());
    }
}

// Http3FrameHandler.java
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http2.Http2HeadersFrame;
import io.netty.incubator.codec.http3.Http3HeadersFrame;

public class Http3FrameHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof Http3HeadersFrame) {
            Http3HeadersFrame http3Frame = (Http3HeadersFrame) msg;

            // Convert HTTP/3 headers to HTTP/2 headers
            Http2HeadersFrame h2Frame = new DefaultHttp2HeadersFrame(http3Frame.headers());

            // Write HTTP/2 frame
            ctx.writeAndFlush(h2Frame);
        }
        ctx.fireChannelRead(msg);
    }
}

// ServerFuture.java
import io.netty.channel.ChannelFuture;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

public class ServerFuture implements GenericFutureListener<Future<? super Void>> {
    @Override
    public void operationComplete(Future<? super Void> future) {
        if (future.isSuccess()) {
            System.out.println("Server started successfully");
        } else {
            System.err.println("Failed to start server: " + future.cause());
        }
    }
}