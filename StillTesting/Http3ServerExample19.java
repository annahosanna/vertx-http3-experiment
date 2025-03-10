import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.incubator.codec.http3.*;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import reactor.core.publisher.Mono;

public class Http3Server {
    private static final int PORT = 8443;

    public static void main(String[] args) throws Exception {
        SelfSignedCertificate cert = new SelfSignedCertificate();
        Http3Configuration config = Http3Configuration.newBuilder()
            .certificateChain(cert.certificate())
            .privateKey(cert.privateKey())
            .build();

        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap b = new Bootstrap();
            b.group(group)
             .channel(Http3ServerChannel.class)
             .handler(new ChannelInitializer<Channel>() {
                @Override
                protected void initChannel(Channel ch) {
                    ch.pipeline().addLast(new FortuneHandler());
                }
             });

            Channel channel = b.bind(PORT).sync().channel();
            channel.closeFuture().sync();
        } finally {
            group.shutdownGracefully();
        }
    }
}

class FortuneHandler extends SimpleChannelInboundHandler<Http3HeadersFrame> {
    private final FortuneService fortuneService = new FortuneService();

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Http3HeadersFrame frame) {
        if (frame.headers().path().equals("/fortune")) {
            fortuneService.getRandomFortune()
                .subscribe(fortune -> {
                    Http3Headers headers = Http3Headers.newHeaders()
                        .status("200")
                        .add("content-type", "application/json");

                    Http3HeadersFrame responseHeaders = Http3HeadersFrame.newHeaders(headers);
                    Http3DataFrame responseData = Http3DataFrame.newFrame(fortune.getBytes());

                    ctx.write(responseHeaders);
                    ctx.writeAndFlush(responseData);
                });
        }
    }
}

class FortuneService {
    private final String[] fortunes = {
        "A beautiful, smart, and loving person will be coming into your life.",
        "A dubious friend may be an enemy in camouflage.",
        "A faithful friend is a strong defense.",
        "A fresh start will put you on your way.",
        "A friend asks only for your time not your money."
    };

    public Mono<String> getRandomFortune() {
        return Mono.just(fortunes[(int)(Math.random() * fortunes.length)]);
    }
}