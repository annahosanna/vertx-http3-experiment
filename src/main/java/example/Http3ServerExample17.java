import io.netty.incubator.codec.http3.*;
import io.reactivex.Single;
import reactor.core.publisher.Mono;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

// RxJava2 Implementation
class RxJavaFortuneServer {
    private final int port;

    public RxJavaFortuneServer(int port) {
        this.port = port;
    }

    public void start() {
        Http3ServerBuilder.create()
            .host("localhost")
            .port(port)
            .handler(new RxJavaFortuneHandler())
            .buildRx()
            .subscribe();
    }
}

class RxJavaFortuneHandler extends SimpleChannelInboundHandler<Http3HeadersFrame> {
    private final FortuneService fortuneService = new FortuneService();

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Http3HeadersFrame frame) {
        Single.fromCallable(() -> fortuneService.getRandomFortune())
            .map(fortune -> {
                ByteBuf content = Unpooled.copiedBuffer(fortune.getBytes());
                Http3HeadersFrame headers = new DefaultHttp3HeadersFrame();
                headers.headers()
                    .status("200")
                    .add("content-type", "text/plain");

                ctx.write(headers);
                return content;
            })
            .subscribe(
                content -> ctx.writeAndFlush(new DefaultHttp3DataFrame(content)),
                error -> {
                    ctx.writeAndFlush(new DefaultHttp3HeadersFrame()
                        .status("500"));
                    ctx.close();
                }
            );
    }
}

// Project Reactor Implementation  
class ReactorFortuneServer {
    private final int port;

    public ReactorFortuneServer(int port) {
        this.port = port;
    }

    public void start() {
        Http3ServerBuilder.create()
            .host("localhost") 
            .port(port)
            .handler(new ReactorFortuneHandler())
            .buildMono()
            .subscribe();
    }
}

class ReactorFortuneHandler extends SimpleChannelInboundHandler<Http3HeadersFrame> {
    private final FortuneService fortuneService = new FortuneService();

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Http3HeadersFrame frame) {
        Mono.fromCallable(() -> fortuneService.getRandomFortune())
            .map(fortune -> {
                ByteBuf content = Unpooled.copiedBuffer(fortune.getBytes());
                Http3HeadersFrame headers = new DefaultHttp3HeadersFrame();
                headers.headers()
                    .status("200")
                    .add("content-type", "text/plain");

                ctx.write(headers);
                return content;
            })
            .subscribe(
                content -> ctx.writeAndFlush(new DefaultHttp3DataFrame(content)),
                error -> {
                    ctx.writeAndFlush(new DefaultHttp3HeadersFrame()
                        .status("500"));
                    ctx.close();
                }
            );
    }
}

class FortuneService {
    private final String[] fortunes = {
        "A beautiful, smart, and loving person will be coming into your life.",
        "A dubious friend may be an enemy in camouflage.",
        "A faithful friend is a strong defense.",
        "A fresh start will put you on your way.",
        "A golden egg of opportunity falls into your lap this month."
    };

    public String getRandomFortune() {
        return fortunes[(int)(Math.random() * fortunes.length)];
    }
}