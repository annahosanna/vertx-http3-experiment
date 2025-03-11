import io.netty.handler.ssl.SslEngine;
import io.netty.incubator.codec.http3.Http3HeadersFrame;
import reactor.netty.quic.QuicServer;
import reactor.core.publisher.Mono;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelHandler;
import io.netty.channel.SimpleChannelInboundHandler;
import java.util.Random;

public class FortuneQuicServer {

    private static final String[] FORTUNES = {
        "You will find happiness in unexpected places",
        "Good fortune will come to you soon",
        "A pleasant surprise is waiting for you",
        "Your hard work will pay off in the future"
    };

    public static void main(String[] args) {
        SslEngine sslEngine = SslContextBuilder
            .forServer()
            .protocols("TLSv1.3")
            .build()
            .newEngine(ByteBufAllocator.DEFAULT);

        QuicServer.create()
            .secure(sslEngine)
            .handle((in, out) -> {
                in.withConnection(conn -> 
                    conn.addHandlerLast("http3Handler", new Http3FortuneHandler()));
                return out.sendString(Mono.just(getRandomFortune()));
            })
            .bind()
            .block();
    }

    private static String getRandomFortune() {
        return FORTUNES[new Random().nextInt(FORTUNES.length)];
    }
}

@ChannelHandler.Sharable
class Http3FortuneHandler extends SimpleChannelInboundHandler<Http3HeadersFrame> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Http3HeadersFrame frame) {
        if (frame.headers().method().equals("GET")) {
            Http3HeadersFrame response = new DefaultHttp3HeadersFrame();
            response.headers()
                .status("200")
                .add("content-type", "text/plain");
            ctx.writeAndFlush(response);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}