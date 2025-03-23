// Required imports
import io.netty.incubator.codec.http3.Http3HeadersFrame;
import io.netty.incubator.codec.http3.DefaultHttp3HeadersFrame;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import reactor.netty.quic.QuicServer;
import reactor.core.publisher.Mono;
import java.util.List;
import java.util.Arrays;
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
            .secure(spec -> spec.sslContext(ssc.certificate(), ssc.privateKey()))
            .handle((request, response) -> {
                return response.sendString(Mono.just(getRandomFortune()));
            })
            .bindNow();
    }

    private static String getRandomFortune() {
        Random rand = new Random();
        return FORTUNES.get(rand.nextInt(FORTUNES.size()));
    }
}

class FortuneHandler {
    public void handleHttp3Headers(Http3HeadersFrame frame) {
        if (frame.headers().method().equals("GET")) {
            DefaultHttp3HeadersFrame responseHeaders = new DefaultHttp3HeadersFrame();
            responseHeaders.headers()
                .status("200")
                .add("content-type", "text/plain");
            // Handle response 
        }
    }
}