<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.example</groupId>
    <artifactId>quic-fortune-server</artifactId>
    <version>1.0-SNAPSHOT</version>

    <properties>
        <netty.version>4.1.75.Final</netty.version>
        <netty.quic.version>0.0.21.Final</netty.quic.version>
        <maven.compiler.source>11</maven.compiler.source>
        <maven.compiler.target>11</maven.compiler.target>
    </properties>

    <dependencies>
        <dependency>
            <groupId>io.netty.incubator</groupId>
            <artifactId>netty-incubator-codec-quic</artifactId>
            <version>${netty.quic.version}</version>
        </dependency>
        <dependency>
            <groupId>io.netty.incubator</groupId>
            <artifactId>netty-incubator-codec-http3</artifactId>
            <version>${netty.quic.version}</version>
        </dependency>
    </dependencies>
</project>

// FortuneServer.java
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.incubator.codec.http3.*;
import io.netty.incubator.codec.quic.*;
import io.netty.handler.ssl.util.SelfSignedCertificate;

import java.net.InetSocketAddress;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class FortuneServer {
    private static final List<String> FORTUNES = Arrays.asList(
        "You will have a great day!",
        "Good luck is coming your way",
        "A surprise awaits around the corner",
        "Trust your instincts"
    );

    private static final Random RANDOM = new Random();

    public static void main(String[] args) throws CertificateException, InterruptedException {
        SelfSignedCertificate cert = new SelfSignedCertificate();
        QuicSslContext sslContext = QuicSslContextBuilder.forServer(cert.privateKey(), cert.certificate())
            .applicationProtocols(Http3.supportedApplicationProtocols()).build();

        NioEventLoopGroup group = new NioEventLoopGroup(1);
        try {
            Bootstrap bootstrap = new Bootstrap()
                .group(group)
                .channel(NioDatagramChannel.class)
                .handler(new QuicServerCodecBuilder()
                    .sslContext(sslContext)
                    .maxIdleTimeout(5000, TimeUnit.MILLISECONDS)
                    .initialMaxData(10000000)
                    .initialMaxStreamDataBidirectionalLocal(1000000)
                    .build());

            Channel channel = bootstrap.bind(new InetSocketAddress(8443)).sync().channel();

            // Add shutdown hook
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                group.shutdownGracefully();
            }));

            channel.closeFuture().sync();
        } finally {
            group.shutdownGracefully();
        }
    }

    private static class FortuneServerHandler extends Http3RequestStreamInboundHandler {
        @Override
        protected void channelRead(ChannelHandlerContext ctx, Http3HeadersFrame frame) throws Exception {
            String fortune = FORTUNES.get(RANDOM.nextInt(FORTUNES.size()));

            Http3Headers headers = new DefaultHttp3Headers()
                .status("200")
                .add("content-type", "text/plain");

            ctx.write(new DefaultHttp3HeadersFrame(headers));
            ctx.writeAndFlush(new DefaultHttp3DataFrame(Unpooled.copiedBuffer(fortune, CharsetUtil.UTF_8)))
                .addListener(ChannelFutureListener.CLOSE);
        }

        @Override
        protected void channelRead(ChannelHandlerContext ctx, Http3DataFrame frame) throws Exception {
            // Release the frame to prevent memory leaks
            frame.release();
        }
    }
}