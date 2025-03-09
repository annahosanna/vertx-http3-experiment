<dependencies>
    <dependency>
        <groupId>io.netty</groupId>
        <artifactId>netty-codec-http3</artifactId>
        <version>4.1.77.Final</version>
    </dependency>
    <dependency>
        <groupId>io.netty.incubator</groupId>
        <artifactId>netty-incubator-codec-quic</artifactId>
        <version>0.0.35.Final</version>
    </dependency>
</dependencies>

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.incubator.codec.quic.*;
import io.netty.handler.ssl.util.SelfSignedCertificate;

public class QuicServer {
    static final int PORT = 8080;

    public static void main(String[] args) throws Exception {
        SelfSignedCertificate cert = new SelfSignedCertificate();
        QuicSslContext context = QuicSslContextBuilder.forServer(cert.privateKey(), cert.certificate())
                .applicationProtocols("http/0.9")
                .build();

        EventLoopGroup group = new QuicEventLoopGroup();
        try {
            ChannelHandler codec = new QuicServerCodecBuilder()
                    .sslContext(context)
                    .maxIdleTimeout(5000, TimeUnit.MILLISECONDS)
                    .initialMaxData(10000000)
                    .initialMaxStreamDataBidirectionalLocal(1000000)
                    .build();

            Bootstrap bs = new Bootstrap();
            Channel channel = bs.group(group)
                    .channel(QuicChannel.class)
                    .handler(codec)
                    .bind(PORT)
                    .sync()
                    .channel();

            channel.closeFuture().sync();
        } finally {
            group.shutdownGracefully();
        }
    }

    private static final class QuicServerHandler extends ChannelInboundHandlerAdapter {
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) {
            if (msg instanceof ByteBuf) {
                ByteBuf buffer = (ByteBuf) msg;
                // Handle received data
                buffer.release();
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            cause.printStackTrace();
            ctx.close();
        }
    }
}