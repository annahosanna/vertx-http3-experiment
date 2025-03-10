import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.incubator.codec.http3.*;
import io.netty.incubator.codec.quic.*;

public class Http3Server {

    public static void main(String[] args) throws Exception {
        QuicServer.start(8443, new Http3ServerInitializer());
    }

    private static class QuicServer {
        static void start(int port, ChannelInitializer initializer) {
            QuicServerCodecBuilder codecBuilder = QuicServerCodecBuilder.create()
                .certificateChain("cert.pem")
                .privateKey("key.pem") 
                .applicationProtocols("h3")
                .maxIdleTimeout(5000, TimeUnit.MILLISECONDS)
                .initialMaxData(10000000)
                .initialMaxStreamDataBidirectionalLocal(1000000)
                .initialMaxStreamDataBidirectionalRemote(1000000)
                .initialMaxStreamsBidirectional(100)
                .handler(initializer);

            try {
                codecBuilder.bind(port).sync().channel().closeFuture().sync();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}

class Http3ServerInitializer extends ChannelInitializer {
    @Override
    protected void initChannel(Channel ch) {
        ChannelPipeline pipeline = ch.pipeline();
        pipeline.addLast(new Http3ServerHandler());
    }
}

class Http3ServerHandler extends SimpleChannelInboundHandler<Http3RequestStreamFrame> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Http3RequestStreamFrame frame) {
        if (frame instanceof Http3HeadersFrame) {
            Http3HeadersFrame headersFrame = (Http3HeadersFrame) frame;

            Http3Headers responseHeaders = new DefaultHttp3Headers()
                .status("200")
                .add("content-type", "text/plain");

            ctx.write(new DefaultHttp3HeadersFrame(responseHeaders));
            ctx.writeAndFlush(new DefaultHttp3DataFrame(Unpooled.copiedBuffer("Hello World", CharsetUtil.UTF_8)));
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}

class Http3Futures {
    private final ChannelFuture bindFuture;
    private final ChannelFuture closeFuture;

    public Http3Futures(ChannelFuture bindFuture, ChannelFuture closeFuture) {
        this.bindFuture = bindFuture;
        this.closeFuture = closeFuture;
    }

    public ChannelFuture getBindFuture() {
        return bindFuture;
    }

    public ChannelFuture getCloseFuture() {
        return closeFuture;
    }
}