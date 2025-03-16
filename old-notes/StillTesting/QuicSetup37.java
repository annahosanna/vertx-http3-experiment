// Import required dependencies
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.incubator.codec.http3.DefaultHttp3DataFrame;
import io.netty.incubator.codec.http3.DefaultHttp3HeadersFrame;
import io.netty.incubator.codec.http3.Http3Frame;
import io.netty.incubator.codec.http3.Http3HeadersFrame;
import reactor.core.publisher.Mono;
import reactor.netty.quic.QuicServer;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import io.netty.handler.ssl.SslContextBuilder;

public class QuicFortuneServer {

    public static void main(String[] args) throws Exception {
        // Create self-signed certificate
        SelfSignedCertificate ssc = new SelfSignedCertificate();

        // Build SSL context
        SslContextBuilder sslBuilder = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey());

        // Create QUIC server
        QuicServer server = QuicServer.create()
            .secure(spec -> spec.sslContext(sslBuilder))
            .host("localhost")
            .port(8443)
            .wiretap(true);

        // Bind handlers and start server
        server.handle((in, out) -> {
            in.withConnection(conn -> conn.addHandlerLast(new Http3FrameHandler()));
            return out.send(Mono.empty());
        }).bind()
        .block();
    }
}

class Http3FrameHandler extends ChannelInboundHandlerAdapter {

    private final String[] fortunes = {
        "A journey of a thousand miles begins with a single step",
        "Fortune favors the bold",
        "The best way to predict the future is to create it"
    };

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof Http3HeadersFrame) {
            Http3HeadersFrame headersFrame = (Http3HeadersFrame) msg;

            // Create response headers
            DefaultHttp3HeadersFrame responseHeaders = new DefaultHttp3HeadersFrame();
            responseHeaders.headers()
                .status("200")
                .add("content-type", "text/plain");

            // Send random fortune
            String fortune = fortunes[(int)(Math.random() * fortunes.length)];
            DefaultHttp3DataFrame responseData = new DefaultHttp3DataFrame(
                ctx.alloc().buffer().writeBytes(fortune.getBytes())
            );

            // Write response
            ctx.write(responseHeaders);
            ctx.write(responseData);
            ctx.flush();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}