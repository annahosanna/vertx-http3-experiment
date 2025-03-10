// Fortune.java
package com.example;

public class Fortune {
    private Long id;
    private String text;

    public Fortune(Long id, String text) {
        this.id = id;
        this.text = text;
    }

    public Long getId() { return id; }
    public String getText() { return text; }
}

// DatabaseHandler.java
package com.example;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHandler {
    private final String DB_URL = "jdbc:h2:~/fortunes";

    public List<Fortune> getFortunes() {
        List<Fortune> fortunes = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id, text FROM fortunes")) {

            while (rs.next()) {
                fortunes.add(new Fortune(rs.getLong("id"), rs.getString("text")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return fortunes;
    }
}

// FortuneHandler.java
package com.example;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.*;
import io.netty.incubator.codec.http3.Http3HeadersFrame;
import io.netty.incubator.codec.http3.Http3DataFrame;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Random;

public class FortuneHandler extends ChannelInboundHandlerAdapter {
    private final DatabaseHandler dbHandler;
    private final ObjectMapper mapper;

    public FortuneHandler() {
        this.dbHandler = new DatabaseHandler();
        this.mapper = new ObjectMapper();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof Http3HeadersFrame) {
            Http3HeadersFrame headersFrame = (Http3HeadersFrame) msg;

            List<Fortune> fortunes = dbHandler.getFortunes();
            Fortune randomFortune = fortunes.get(new Random().nextInt(fortunes.size()));

            byte[] jsonBytes = mapper.writeValueAsBytes(randomFortune);

            Http3HeadersFrame responseHeaders = new DefaultHttp3HeadersFrame();
            responseHeaders.headers()
                .status("200")
                .add(HttpHeaderNames.CONTENT_TYPE, "application/json")
                .add(HttpHeaderNames.CONTENT_LENGTH, jsonBytes.length);

            ctx.write(responseHeaders);

            ByteBuf content = ctx.alloc().buffer();
            content.writeBytes(jsonBytes);
            ctx.writeAndFlush(new DefaultHttp3DataFrame(content));
        }
    }
}

// FortuneServer.java
package com.example;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.incubator.codec.quic.*;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import java.security.cert.CertificateException;

public class FortuneServer {
    private final int port;

    public FortuneServer(int port) {
        this.port = port;
    }

    public void start() throws CertificateException {
        EventLoopGroup group = new NioEventLoopGroup();

        try {
            SelfSignedCertificate cert = new SelfSignedCertificate();

            QuicServerCodecBuilder codecBuilder = QuicServerCodecBuilder.create()
                .certificate(cert.certificate())
                .privateKey(cert.privateKey())
                .handler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel ch) {
                        ch.pipeline().addLast(new FortuneHandler());
                    }
                });

            ServerBootstrap b = new ServerBootstrap();
            b.group(group)
                .channel(QuicServerChannel.class)
                .handler(codecBuilder.build())
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) {
                        ch.pipeline().addLast(new FortuneHandler());
                    }
                });

            ChannelFuture f = b.bind(port).sync();
            f.channel().closeFuture().sync();

        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            group.shutdownGracefully();
        }
    }

    public static void main(String[] args) throws CertificateException {
        new FortuneServer(8443).start();
    }
}