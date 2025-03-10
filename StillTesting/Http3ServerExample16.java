// Server 1 - RxJava2 Implementation
import io.netty.incubator.codec.http3.*;
import io.reactivex.Flowable;
import io.netty.channel.*;

public class RxJavaHttp3Server {
    private static final int PORT = 8443;

    public static void main(String[] args) throws Exception {
        QuicSslContext sslContext = QuicSslContextBuilder.forServer("cert.pem", "key.pem").build();

        Http3ServerBuilder.create()
            .sslContext(sslContext)
            .handler(new ChannelInitializer<Channel>() {
                @Override
                protected void initChannel(Channel ch) {
                    ch.pipeline().addLast(new Http3ServerHandler());
                }
            })
            .bind(PORT)
            .sync()
            .channel()
            .closeFuture()
            .sync();
    }

    static class Http3ServerHandler extends Http3RequestStreamInboundHandler {
        private static final String[] FORTUNES = {
            "A journey of a thousand miles begins with a single step",
            "Fortune favors the bold",
            "The best way to predict the future is to create it"
        };

        @Override
        protected void channelRead(ChannelHandlerContext ctx, Http3HeadersFrame headersFrame) {
            Flowable.just(FORTUNES[(int)(Math.random() * FORTUNES.length)])
                .map(fortune -> {
                    Http3Headers headers = Http3Headers.newHeaders();
                    headers.status("200");
                    headers.add("content-type", "text/plain");

                    ctx.write(new DefaultHttp3HeadersFrame(headers));
                    return fortune;
                })
                .subscribe(fortune -> {
                    ctx.writeAndFlush(new DefaultHttp3DataFrame(
                        ctx.alloc().buffer().writeBytes(fortune.getBytes())));
                });
        }
    }
}

// Server 2 - Project Reactor Implementation 
import io.netty.incubator.codec.http3.*;
import reactor.core.publisher.Mono;
import io.netty.channel.*;

public class ReactorHttp3Server {
    private static final int PORT = 8444;

    public static void main(String[] args) throws Exception {
        QuicSslContext sslContext = QuicSslContextBuilder.forServer("cert.pem", "key.pem").build();

        Http3ServerBuilder.create()
            .sslContext(sslContext)
            .handler(new ChannelInitializer<Channel>() {
                @Override
                protected void initChannel(Channel ch) {
                    ch.pipeline().addLast(new Http3ServerHandler());
                }
            })
            .bind(PORT)
            .sync()
            .channel()
            .closeFuture()
            .sync();
    }

    static class Http3ServerHandler extends Http3RequestStreamInboundHandler {
        private static final String[] FORTUNES = {
            "Life is what happens while you're busy making other plans",
            "Today is the first day of the rest of your life",
            "The only constant in life is change"
        };

        @Override
        protected void channelRead(ChannelHandlerContext ctx, Http3HeadersFrame headersFrame) {
            Mono.just(FORTUNES[(int)(Math.random() * FORTUNES.length)])
                .map(fortune -> {
                    Http3Headers headers = Http3Headers.newHeaders();
                    headers.status("200");
                    headers.add("content-type", "text/plain");

                    ctx.write(new DefaultHttp3HeadersFrame(headers));
                    return fortune;
                })
                .subscribe(fortune -> {
                    ctx.writeAndFlush(new DefaultHttp3DataFrame(
                        ctx.alloc().buffer().writeBytes(fortune.getBytes())));
                });
        }
    }
}