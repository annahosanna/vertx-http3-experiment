import io.netty.incubator.codec.http3.Http3HeadersFrame;
import io.netty.incubator.codec.http3.Http3RequestStreamInboundHandler;
import reactor.core.publisher.Mono;
import reactor.netty.quic.QuicServer;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import java.security.cert.CertificateException;
import java.util.List;
import java.util.Random;

public class QuicFortuneServer {

    static final List<String> FORTUNES = List.of(
        "Today is your lucky day!",
        "Good things come to those who wait",
        "A journey of a thousand miles begins with a single step"
    );

    public static void main(String[] args) throws CertificateException {
        SelfSignedCertificate cert = new SelfSignedCertificate();

        QuicServer.create()
            .secure(spec -> spec.certificate(cert.certificate())
                              .privateKey(cert.privateKey()))
            .handle((in, out) -> {
                in.withConnection(conn -> 
                    conn.addHandlerLast(new FortuneHandler()));
                return out.sendString(Mono.just(getRandomFortune()));
            })
            .bindNow()
            .onDispose()
            .block();
    }

    private static String getRandomFortune() {
        return FORTUNES.get(new Random().nextInt(FORTUNES.size()));
    }
}

class FortuneHandler extends Http3RequestStreamInboundHandler {

    @Override
    protected void channelRead(ChannelHandlerContext ctx, Http3HeadersFrame frame) {
        System.out.println("Received headers: " + frame.headers());
    }

    @Override
    protected void channelRead(ChannelHandlerContext ctx, Http3DataFrame frame) {
        System.out.println("Received data frame");
    }

    @Override 
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }
}