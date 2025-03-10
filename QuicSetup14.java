// File: QuicServer.java
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.incubator.codec.quic.QuicServerCodecBuilder;
import io.netty.incubator.codec.quic.QuicChannel;
import io.netty.incubator.codec.quic.QuicServerChannel;
import io.netty.incubator.codec.quic.QuicStreamChannel;

public class QuicServer {
    private final int port;

    public QuicServer(int port) {
        this.port = port;
    }

    public void start() throws Exception {
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(group)
             .channel(QuicServerChannel.class)
             .childHandler(new ChannelInitializer<QuicChannel>() {
                @Override
                protected void initChannel(QuicChannel ch) {
                    ch.pipeline().addLast(new Http3ServerHandler());
                }
             })
             .option(ChannelOption.SO_BACKLOG, 128);

            QuicServerCodecBuilder codecBuilder = QuicServerCodecBuilder.create()
                .certificateChain("cert.pem")
                .privateKey("key.pem")
                .applicationProtocols("h3");

            ChannelFuture f = b.bind(port).sync();
            f.channel().closeFuture().sync();
        } finally {
            group.shutdownGracefully();
        }
    }
}

// File: Http3ServerHandler.java
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.incubator.codec.http3.Http3DataFrame;
import io.netty.incubator.codec.http3.Http3HeadersFrame;
import io.netty.incubator.codec.http3.DefaultHttp3HeadersFrame;
import io.netty.incubator.codec.http3.DefaultHttp3DataFrame;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class Http3ServerHandler extends ChannelInboundHandlerAdapter {
    private ByteBuf content = Unpooled.buffer();

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof Http3HeadersFrame) {
            // Process headers
        } else if (msg instanceof Http3DataFrame) {
            Http3DataFrame dataFrame = (Http3DataFrame) msg;
            content.writeBytes(dataFrame.content());

            if (dataFrame.isEndStream()) {
                // Stream fully received
                Http3ResponseHandler.sendResponse(ctx);
                content.release();
            }
        }
    }
}

// File: Http3ResponseHandler.java
import io.netty.channel.ChannelHandlerContext;
import io.netty.incubator.codec.http3.DefaultHttp3Headers;
import io.netty.incubator.codec.http3.Http3Headers;
import io.netty.incubator.codec.http3.DefaultHttp3HeadersFrame;

public class Http3ResponseHandler {
    public static void sendResponse(ChannelHandlerContext ctx) {
        Http3Headers headers = new DefaultHttp3Headers();
        headers.status("200");
        headers.add("content-type", "text/plain");

        ctx.writeAndFlush(new DefaultHttp3HeadersFrame(headers))
           .addListener(future -> {
                if (future.isSuccess()) {
                    ctx.close();
                }
           });
    }
}