// Required Imports
import io.netty.incubator.codec.http3.Http3HeadersFrame;
import io.netty.incubator.codec.http3.Http3ServerConnectionHandler;
import io.netty.handler.ssl.ApplicationProtocolConfig;
import io.netty.handler.ssl.SslContextBuilder;
import reactor.core.publisher.Mono;
import reactor.netty.quic.QuicServer;

// Custom handler classes
class FortuneHeaderFrameHandler extends Http3ServerConnectionHandler {
    private static final String[] FORTUNES = {
        "Today is your lucky day!",
        "Good things come to those who wait",
        "A pleasant surprise is coming your way"
    };

    @Override 
    protected void channelRead0(ChannelHandlerContext ctx, Http3HeadersFrame frame) {
        String fortune = FORTUNES[new Random().nextInt(FORTUNES.length)];
        Http3HeadersFrame response = new DefaultHttp3HeadersFrame()
            .status(HttpResponseStatus.OK.codeAsText())
            .add("content-type", "text/plain")
            .add("content-length", fortune.length());

        ctx.writeAndFlush(response);
        ctx.writeAndFlush(new DefaultHttp3DataFrame(Unpooled.wrappedBuffer(fortune.getBytes())));
    }
}

class MainServer {
    public Mono<Void> start() {
        SslContext sslContext = SslContextBuilder.forServer(certFile, keyFile)
            .applicationProtocolConfig(new ApplicationProtocolConfig(
                ApplicationProtocolConfig.Protocol.ALPN,
                ApplicationProtocolConfig.SelectorFailureBehavior.NO_ADVERTISE,
                ApplicationProtocolConfig.SelectedListenerFailureBehavior.ACCEPT,
                ApplicationProtocolNames.HTTP_3))
            .build();

        QuicServer quicServer = QuicServer.create()
            .secure(sslContext)
            .port(8443)
            .wiretap(true);

        return quicServer.bind()
            .doOnConnection(conn -> 
                conn.addHandlerLast("http3Handler", new Http3ServerConnectionHandler())
                    .addHandlerLast("fortuneHandler", new FortuneHeaderFrameHandler())
            );
    }
}