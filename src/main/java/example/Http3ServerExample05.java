import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.incubator.codec.http3.*;
import io.netty.incubator.codec.quic.*;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;

public class FortuneClient {
    private static final String HOST = "localhost";
    private static final int PORT = 8443;

    public static void main(String[] args) throws Exception {
        QuicSslContext context = QuicSslContextBuilder.forClient()
            .trustManager(InsecureTrustManagerFactory.INSTANCE)
            .applicationProtocols("h3")
            .build();

        NioEventLoopGroup group = new NioEventLoopGroup(1);
        try {
            Bootstrap bootstrap = new Bootstrap()
                .group(group)
                .channel(NioDatagramChannel.class)
                .handler(QuicClientCodecBuilder.create()
                    .sslContext(context)
                    .maxIdleTimeout(5000, TimeUnit.MILLISECONDS)
                    .initialMaxData(10000000)
                    .initialMaxStreamDataBidirectionalLocal(1000000)
                    .build());

            Channel channel = bootstrap.connect(HOST, PORT).sync().channel();
            QuicChannel quicChannel = QuicChannel.newBootstrap(channel)
                .streamHandler(new ChannelInitializer<QuicStreamChannel>() {
                    @Override
                    protected void initChannel(QuicStreamChannel ch) {
                        ch.pipeline().addLast(new Http3FrameCodec())
                            .addLast(new Http3RequestStreamHandler());
                    }
                })
                .remoteAddress(HOST, PORT)
                .sync()
                .get();

            Http3RequestStreamChannel streamChannel = quicChannel.createStream(
                QuicStreamType.BIDIRECTIONAL,
                new Http3RequestStreamHandler())
                .sync().get();

            DefaultFullHttpRequest request = new DefaultFullHttpRequest(
                HttpVersion.valueOf("HTTP/3.0"),
                HttpMethod.GET,
                "/fortune");

            streamChannel.writeAndFlush(request)
                .addListener(future -> {
                    if (!future.isSuccess()) {
                        future.cause().printStackTrace();
                    }
                });

            streamChannel.closeFuture().sync();
            quicChannel.closeFuture().sync();

        } finally {
            group.shutdownGracefully();
        }
    }

    private static class Http3RequestStreamHandler extends ChannelInboundHandlerAdapter {
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) {
            if (msg instanceof Http3HeadersFrame) {
                Http3HeadersFrame headersFrame = (Http3HeadersFrame) msg;
                System.out.println("Received headers: " + headersFrame.headers());
            } else if (msg instanceof Http3DataFrame) {
                Http3DataFrame dataFrame = (Http3DataFrame) msg;
                System.out.println("Received data: " + 
                    dataFrame.content().toString(CharsetUtil.UTF_8));
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            cause.printStackTrace();
            ctx.close();
        }
    }
}