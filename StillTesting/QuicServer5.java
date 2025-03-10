import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import io.netty.incubator.codec.quic.*;
import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;

public class QuicServer {
    private static final int PORT = 8443;

    public static void main(String[] args) throws Exception {
        SelfSignedCertificate ssc = new SelfSignedCertificate();
        SslContext sslContext = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey()).build();

        NioEventLoopGroup group = new NioEventLoopGroup(1);
        QuicServerCodecBuilder serverCodecBuilder = QuicServerCodecBuilder.create()
            .sslContext(sslContext)
            .maxIdleTimeout(5000, TimeUnit.MILLISECONDS)
            .initialMaxData(10000000)
            .initialMaxStreamDataBidirectionalLocal(1000000)
            .initialMaxStreamDataBidirectionalRemote(1000000)
            .initialMaxStreamsBidirectional(100);

        try {
            ServerBootstrap bootstrap = new ServerBootstrap()
                .group(group)
                .channel(NioServerSocketChannel.class)
                .handler(serverCodecBuilder.build())
                .childHandler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel ch) {
                        ch.pipeline().addLast(new QuicServerHandler());
                    }
                });

            DisposableServer server = Mono.fromFuture(bootstrap.bind(PORT).sync())
                .map(channel -> new DisposableServer() {
                    @Override
                    public void dispose() {
                        channel.close();
                    }

                    @Override
                    public Mono<Void> onDispose() {
                        return Mono.fromFuture(channel.closeFuture());
                    }
                })
                .block();

            System.out.println("QUIC Server started on port " + PORT);
            server.onDispose().block();
        } finally {
            group.shutdownGracefully();
        }
    }
}

class QuicServerHandler extends QuicStreamFrameTypeValidator<QuicStreamChannel> {
    @Override
    protected void channelRead(ChannelHandlerContext ctx, QuicStreamFrame frame) {
        byte[] data = new byte[frame.content().readableBytes()];
        frame.content().readBytes(data);
        System.out.println("Received: " + new String(data));

        // Echo the data back
        QuicStreamChannel channel = (QuicStreamChannel) ctx.channel();
        channel.writeAndFlush(new DefaultQuicStreamFrame(Unpooled.wrappedBuffer(data)));
    }
}