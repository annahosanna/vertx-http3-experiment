// Required imports
import io.netty.incubator.codec.http3.DefaultHttp3HeadersFrame;
import io.netty.incubator.codec.http3.Http3DataFrame;
import io.netty.incubator.codec.http3.Http3HeadersFrame;
import io.netty.incubator.codec.http3.Http3FrameCodec;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import io.netty.handler.ssl.SslContextBuilder;
import reactor.netty.incubator.quic.QuicServer;
import reactor.core.publisher.Mono;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import java.util.List;
import java.util.Random;

public class QuicFortuneServer {

    private static final List<String> FORTUNES = List.of(
        "A beautiful, smart, and loving person will be coming into your life.",
        "A dubious friend may be an enemy in camouflage.",
        "A faithful friend is a strong defense.",
        "A fresh start will put you on your way."
    );

    private static final Random RANDOM = new Random();

    public static void main(String[] args) throws Exception {
        SelfSignedCertificate ssc = new SelfSignedCertificate();
        SslContextBuilder sslContextBuilder = SslContextBuilder
            .forServer(ssc.certificate(), ssc.privateKey())
            .protocols("TLSv1.3");

        QuicServer.create()
            .secure(spec -> spec.sslContext(sslContextBuilder))
            .protocol(new Http3FrameCodec())
            .handle((in, out) -> {
                in.withConnection(conn -> 
                    conn.addHandlerLast("http3Handler", new Http3FortuneHandler()));
                return out.neverComplete();
            })
            .bind();
    }
}

class Http3FortuneHandler extends SimpleChannelInboundHandler<Http3HeadersFrame> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Http3HeadersFrame frame) {
        String fortune = QuicFortuneServer.FORTUNES.get(
            QuicFortuneServer.RANDOM.nextInt(QuicFortuneServer.FORTUNES.size())
        );

        Http3HeadersFrame response = new DefaultHttp3HeadersFrame();
        response.headers()
            .status("200")
            .add("content-type", "text/plain");

        ctx.writeAndFlush(response);
        ctx.writeAndFlush(new Http3DataFrame(fortune));
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}