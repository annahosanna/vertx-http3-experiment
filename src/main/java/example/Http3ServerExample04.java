Example 1: Simple HTTP/3 Fortune Server

pom.xml:
<dependencies>
    <dependency>
        <groupId>io.netty.incubator</groupId>
        <artifactId>netty-incubator-codec-http3</artifactId>
        <version>0.0.17.Final</version>
    </dependency>
</dependencies>

FortuneServer.java:
import io.netty.bootstrap.ServerBootstrap; 
import io.netty.channel.*;
import io.netty.incubator.codec.http3.*;
import io.netty.handler.ssl.util.SelfSignedCertificate;

public class FortuneServer {
    public static void main(String[] args) throws Exception {
        SelfSignedCertificate ssc = new SelfSignedCertificate();

        ServerBootstrap b = new ServerBootstrap();
        b.group(new NioEventLoopGroup())
         .channel(Http3ServerChannel.class)
         .childHandler(new ChannelInitializer<Channel>() {
             @Override
             protected void initChannel(Channel ch) {
                 ch.pipeline().addLast(new Http3ServerHandler());
             }
         });

        Channel ch = b.bind(8080).sync().channel();
        ch.closeFuture().sync();
    }
}

Example 2: Fortune Server with Custom Handler

pom.xml:
<dependencies>
    <dependency>
        <groupId>io.netty.incubator</groupId>
        <artifactId>netty-incubator-codec-http3</artifactId>
        <version>0.0.17.Final</version>
    </dependency>
</dependencies>

FortuneHandler.java:
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.incubator.codec.http3.*;
import io.netty.handler.codec.http.*;

public class FortuneHandler extends ChannelInboundHandlerAdapter {
    private static final String[] FORTUNES = {
        "A journey of a thousand miles begins with a single step",
        "Fortune favors the bold",
        "The best is yet to come"
    };

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof Http3HeadersFrame) {
            String fortune = FORTUNES[(int)(Math.random() * FORTUNES.length)];
            ByteBuf content = Unpooled.copiedBuffer(fortune.getBytes());

            Http3Headers headers = Http3Headers.newHeaders();
            headers.status("200");
            headers.add("content-type", "text/plain");

            ctx.write(new DefaultHttp3HeadersFrame(headers));
            ctx.write(new DefaultHttp3DataFrame(content));
            ctx.flush();
        }
    }
}

Example 3: Async Fortune Server

pom.xml:
<dependencies>
    <dependency>
        <groupId>io.netty.incubator</groupId>
        <artifactId>netty-incubator-codec-http3</artifactId>
        <version>0.0.17.Final</version>
    </dependency>
</dependencies>

AsyncFortuneServer.java:
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.incubator.codec.http3.*;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import java.util.concurrent.CompletableFuture;

public class AsyncFortuneServer {
    public static void main(String[] args) throws Exception {
        ServerBootstrap b = new ServerBootstrap();
        b.group(new NioEventLoopGroup())
         .channel(Http3ServerChannel.class)
         .childHandler(new ChannelInitializer<Channel>() {
             @Override
             protected void initChannel(Channel ch) {
                 ch.pipeline().addLast(new AsyncFortuneHandler());
             }
         });

        Channel ch = b.bind(8080).sync().channel();
        ch.closeFuture().sync();
    }
}

class AsyncFortuneHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof Http3HeadersFrame) {
            CompletableFuture.supplyAsync(() -> generateFortune())
                .thenAccept(fortune -> {
                    Http3Headers headers = Http3Headers.newHeaders();
                    headers.status("200");
                    headers.add("content-type", "text/plain");

                    ctx.write(new DefaultHttp3HeadersFrame(headers));
                    ctx.write(new DefaultHttp3DataFrame(
                        Unpooled.copiedBuffer(fortune.getBytes())));
                    ctx.flush();
                });
        }
    }

    private String generateFortune() {
        try {
            Thread.sleep(100); // Simulate async work
            return "Your future is bright!";
        } catch (InterruptedException e) {
            return "Cannot predict now";
        }
    }
}