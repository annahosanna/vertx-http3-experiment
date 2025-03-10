import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.incubator.codec.quic.*;
import io.netty.incubator.codec.http3.*;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.net.InetSocketAddress;
import java.security.cert.CertificateException;

public class QuicServer {
    private final int port;

    public QuicServer(int port) {
        this.port = port;
    }

    public void start() throws CertificateException {
        SelfSignedCertificate cert = new SelfSignedCertificate();
        EventLoopGroup group = new NioEventLoopGroup(1);

        try {
            QuicServerCodecBuilder serverBuilder = new QuicServerCodecBuilder()
                .certificate(cert.certificate())
                .privateKey(cert.privateKey())
                .handler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel ch) {
                        ch.pipeline().addLast(new Http3ServerHandler())
                                   .addLast(new StreamFutureHandler())
                                   .addLast(new HeadersHandler());
                    }
                });

            Channel server = serverBuilder.bind(new InetSocketAddress(port))
                .sync()
                .channel();

            server.closeFuture().sync();

        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            group.shutdownGracefully();
        }
    }
}

class Http3ServerHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof Http3HeadersFrame) {
            Http3HeadersFrame headersFrame = (Http3HeadersFrame) msg;
            ctx.fireChannelRead(headersFrame);
        } else if (msg instanceof Http3DataFrame) {
            Http3DataFrame dataFrame = (Http3DataFrame) msg;
            ctx.fireChannelRead(dataFrame);
        }
    }
}

class StreamFutureHandler extends ChannelInboundHandlerAdapter {
    private ByteBuf aggregatedContent = Unpooled.buffer();

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof Http3DataFrame) {
            Http3DataFrame dataFrame = (Http3DataFrame) msg;
            aggregatedContent.writeBytes(dataFrame.content());

            if (dataFrame.isEndStream()) {
                ctx.fireChannelRead(aggregatedContent);
                aggregatedContent = Unpooled.buffer();
            }
        }
    }
}

class HeadersHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof ByteBuf) {
            Http3Headers responseHeaders = new DefaultHttp3Headers()
                .status("200")
                .add("content-type", "text/plain");

            ctx.writeAndFlush(new DefaultHttp3HeadersFrame(responseHeaders));
        }
    }
}