// Imports
import io.netty.incubator.codec.http3.Http3HeadersFrame;
import reactor.core.publisher.Mono;
import reactor.netty.quic.QuicServer;
import io.netty.incubator.codec.http3.Http3;
import io.netty.incubator.codec.http3.Http3FrameCodec;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class QuicFortuneServer {

    private static final List<String> FORTUNES = Arrays.asList(
        "A journey of a thousand miles begins with a single step",
        "Fortune favors the bold",
        "The future belongs to those who believe in the beauty of their dreams"
    );

    public static void main(String[] args) throws Exception {
        SelfSignedCertificate ssc = new SelfSignedCertificate();

        QuicServer.create()
            .secure(spec -> spec.keyCertChain(ssc.certificate())
                              .key(ssc.privateKey()))
            .handle((in, out) -> {
                in.withConnection(conn -> {
                    conn.addHandlerLast(new Http3FrameCodec());
                    conn.addHandlerLast(new FortuneHandler());
                });

                return out.withConnection(conn -> {
                    return Mono.empty();
                });
            })
            .bind()
            .block();
    }
}

class FortuneHandler {
    private final Random random = new Random();

    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof Http3HeadersFrame) {
            Http3HeadersFrame headers = (Http3HeadersFrame) msg;

            String fortune = FORTUNES.get(random.nextInt(FORTUNES.size()));

            Http3HeadersFrame response = Http3.newHeadersFrame()
                .status("200")
                .add("content-type", "text/plain")
                .add("content-length", String.valueOf(fortune.length()));

            ctx.write(response);
            ctx.writeAndFlush(fortune);
        }
    }
}