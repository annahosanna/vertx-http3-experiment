pom.xml:
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.example</groupId>
    <artifactId>http3-fortune-server</artifactId>
    <version>1.0-SNAPSHOT</version>

    <properties>
        <maven.compiler.source>11</maven.compiler.source>
        <maven.compiler.target>11</maven.compiler.target>
        <netty.version>4.1.86.Final</netty.version>
        <netty.incubator.version>0.0.15.Final</netty.incubator.version>
        <rxjava.version>2.2.21</rxjava.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>io.netty.incubator</groupId>
            <artifactId>netty-incubator-codec-http3</artifactId>
            <version>${netty.incubator.version}</version>
        </dependency>
        <dependency>
            <groupId>io.reactivex.rxjava2</groupId>
            <artifactId>rxjava</artifactId>
            <version>${rxjava.version}</version>
        </dependency>
    </dependencies>
</project>

FortuneServer.java:
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.incubator.codec.http3.*;

public class FortuneServer {
    private final int port;

    public FortuneServer(int port) {
        this.port = port;
    }

    public void start() throws Exception {
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ChannelPipeline p = ch.pipeline();
                        p.addLast(new Http3ServerConnectionHandler());
                        p.addLast(new FortuneHandler());
                    }
                });

            Channel ch = b.bind(port).sync().channel();
            System.out.println("HTTP/3 Server started on port " + port);
            ch.closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    public static void main(String[] args) throws Exception {
        int port = 8443;
        new FortuneServer(port).start();
    }
}

FortuneHandler.java:
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.incubator.codec.http3.*;
import io.reactivex.Single;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class FortuneHandler extends SimpleChannelInboundHandler<Http3HeadersFrame> {
    private final List<String> fortunes = Arrays.asList(
        "You will have a great day!",
        "A surprise is waiting for you.",
        "Good fortune will be yours.",
        "Success is in your future.",
        "Adventure awaits around the corner."
    );

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Http3HeadersFrame frame) {
        getFortune()
            .subscribe(fortune -> sendResponse(ctx, fortune));
    }

    private Single<String> getFortune() {
        return Single.fromCallable(() -> {
            Random random = new Random();
            return fortunes.get(random.nextInt(fortunes.size()));
        });
    }

    private void sendResponse(ChannelHandlerContext ctx, String fortune) {
        Http3Headers headers = Http3Headers.newHeaders()
            .status("200")
            .add("content-type", "text/plain");

        ctx.write(new DefaultHttp3HeadersFrame(headers));

        ByteBuf content = Unpooled.copiedBuffer(fortune, StandardCharsets.UTF_8);
        ctx.write(new DefaultHttp3DataFrame(content));

        ctx.writeAndFlush(new DefaultHttp3DataFrame(Unpooled.EMPTY_BUFFER, true));
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}