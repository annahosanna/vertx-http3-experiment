import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http3.*;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;

public class Http3Server {
    private final int port;

    public Http3Server(int port) {
        this.port = port;
    }

    public void start() throws Exception {
        // Configure SSL
        SelfSignedCertificate ssc = new SelfSignedCertificate();
        SslContext sslCtx = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey())
            .applicationProtocolConfig(Http3.ALPN)
            .build();

        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ChannelPipeline p = ch.pipeline();
                        p.addLast(sslCtx.newHandler(ch.alloc()));
                        p.addLast(new Http3ServerCodec());
                        p.addLast(new Http3ServerHandler());
                    }
                });

            Channel ch = b.bind(port).sync().channel();
            System.out.println("HTTP/3 Server started on port " + port);
            ch.closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    private static class Http3ServerHandler extends SimpleChannelInboundHandler<Http3HeadersFrame> {
        @Override
        protected void channelRead0(ChannelHandlerContext ctx, Http3HeadersFrame frame) {
            Http3Headers headers = new DefaultHttp3Headers();
            headers.status("200");
            headers.set("content-type", "text/plain");

            Http3HeadersFrame responseHeaders = new DefaultHttp3HeadersFrame(headers);
            ctx.write(responseHeaders);

            Http3DataFrame data = new DefaultHttp3DataFrame("Hello HTTP/3!".getBytes());
            ctx.write(data);

            ctx.flush();
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            cause.printStackTrace();
            ctx.close();
        }
    }

    public static void main(String[] args) throws Exception {
        int port = 8443;
        new Http3Server(port).start();
    }
}