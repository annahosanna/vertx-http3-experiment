import io.netty.bootstrap.Bootstrap;
import io.netty.incubator.codec.http3.*;
import io.netty.channel.*;
// RxJava2
import io.reactivex.Single;
import io.reactivex.subjects.PublishSubject;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class Http3FortuneServer {
    private static final List<String> FORTUNES = Arrays.asList(
        "A journey of a thousand miles begins with a single step",
        "Fortune favors the bold",
        "The future belongs to those who believe in the beauty of their dreams"
    );

    private final int port;
    private final PublishSubject<String> fortuneSubject = PublishSubject.create();

    public Http3FortuneServer(int port) {
        this.port = port;
    }

    public void start() throws Exception {
        EventLoopGroup group = new DefaultEventLoopGroup();
        try {
            Bootstrap b = new Bootstrap();
            b.group(group)
             .channel(Http3ServerChannel.class)
             .handler(new Http3ServerInitializer());

            Channel ch = b.bind(port).sync().channel();
            ch.closeFuture().sync();
        } finally {
            group.shutdownGracefully();
        }
    }
}

class Http3ServerInitializer extends ChannelInitializer<Channel> {
    @Override
    protected void initChannel(Channel ch) {
        ChannelPipeline p = ch.pipeline();
        p.addLast(new Http3ServerCodec());
        p.addLast(new FortuneHandler());
    }
}

class FortuneHandler extends SimpleChannelInboundHandler<Http3HeadersFrame> {
    private final FortuneService fortuneService = new FortuneService();

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Http3HeadersFrame frame) {
        if (frame.headers().method().equalsIgnoreCase("GET")) {
            fortuneService.getRandomFortune()
                .subscribe(
                    fortune -> sendResponse(ctx, fortune),
                    error -> sendError(ctx, error)
                );
        }
    }

    private void sendResponse(ChannelHandlerContext ctx, String fortune) {
        Http3Headers headers = Http3Headers.newHeaders()
            .status("200")
            .add("content-type", "text/plain");

        ctx.write(new DefaultHttp3HeadersFrame(headers));
        ctx.writeAndFlush(new DefaultHttp3DataFrame(ctx.alloc().buffer().writeBytes(fortune.getBytes())));
    }

    private void sendError(ChannelHandlerContext ctx, Throwable error) {
        Http3Headers headers = Http3Headers.newHeaders()
            .status("500");
        ctx.writeAndFlush(new DefaultHttp3HeadersFrame(headers));
    }
}

class FortuneService {
    private final Random random = new Random();

    public Single<String> getRandomFortune() {
        return Single.fromCallable(() -> {
            int index = random.nextInt(Http3FortuneServer.FORTUNES.size());
            return Http3FortuneServer.FORTUNES.get(index);
        });
    }
}