<dependencies>
    <dependency>
        <groupId>io.netty</groupId>
        <artifactId>netty-all</artifactId>
        <version>4.1.86.Final</version>
    </dependency>
</dependencies>

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.incubator.codec.quic.*;

public class QuicServerExample {
    public static void main(String[] args) throws Exception {
        QuicSslContext sslContext = QuicSslContextBuilder.forServer(
                new File("cert.pem"), new File("key.pem"))
                .applicationProtocols("http/0.9")
                .build();

        EventLoopGroup group = new NioEventLoopGroup(1);
        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(group)
                    .channel(NioDatagramChannel.class)
                    .handler(new ChannelInitializer<Channel>() {
                        @Override
                        protected void initChannel(Channel ch) {
                            ChannelPipeline pipeline = ch.pipeline();
                            pipeline.addLast(new QuicServerCodecBuilder()
                                    .sslContext(sslContext)
                                    .maxIdleTimeout(5000, TimeUnit.MILLISECONDS)
                                    .initialMaxData(10000000)
                                    .initialMaxStreamDataBidirectionalLocal(1000000)
                                    .build());
                            pipeline.addLast(new QuicServerHandler());
                        }
                    });

            Channel channel = bootstrap.bind(8080).sync().channel();
            channel.closeFuture().sync();
        } finally {
            group.shutdownGracefully();
        }
    }
}

class QuicServerHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof QuicStreamChannel) {
            QuicStreamChannel streamChannel = (QuicStreamChannel) msg;
            streamChannel.pipeline().addLast(new QuicStreamHandler());
        }
    }
}

class QuicStreamHandler extends SimpleChannelInboundHandler<ByteBuf> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) {
        // Handle incoming data
        byte[] data = new byte[msg.readableBytes()];
        msg.readBytes(data);
        System.out.println("Received: " + new String(data));

        // Echo back
        ctx.writeAndFlush(Unpooled.copiedBuffer("Server received: " + new String(data)));
    }
}