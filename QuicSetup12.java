// Main.java
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.incubator.codec.quic.QuicServerCodecBuilder;
import io.netty.incubator.codec.quic.QuicChannel;
import io.netty.incubator.codec.quic.QuicSslContext;
import io.netty.incubator.codec.quic.QuicSslContextBuilder;

public class Main {
    public static void main(String[] args) throws Exception {
        QuicSslContext sslContext = QuicSslContextBuilder.forServer("key.pem", "cert.pem").build();

        NioEventLoopGroup group = new NioEventLoopGroup(1);

        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(group)
             .channel(NioServerSocketChannel.class)
             .childHandler(new QuicServerInitializer(sslContext));

            Channel channel = b.bind(8080).sync().channel();
            channel.closeFuture().sync();
        } finally {
            group.shutdownGracefully();
        }
    }
}

// QuicServerInitializer.java
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.incubator.codec.quic.QuicServerCodecBuilder;
import io.netty.incubator.codec.quic.QuicSslContext;

public class QuicServerInitializer extends ChannelInitializer<NioSocketChannel> {
    private final QuicSslContext sslContext;

    public QuicServerInitializer(QuicSslContext sslContext) {
        this.sslContext = sslContext;
    }

    @Override
    protected void initChannel(NioSocketChannel ch) {
        QuicServerCodecBuilder.create()
            .sslContext(sslContext)
            .handler(new QuicChannelInitializer())
            .build()
            .addChannelHandlerFirst(ch);
    }
}

// QuicChannelInitializer.java
import io.netty.channel.ChannelInitializer;
import io.netty.incubator.codec.quic.QuicChannel;
import io.netty.incubator.codec.http3.Http3FrameCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;

public class QuicChannelInitializer extends ChannelInitializer<QuicChannel> {
    @Override
    protected void initChannel(QuicChannel ch) {
        ch.pipeline()
          .addLast(new Http3FrameCodec())
          .addLast(new HttpObjectAggregator(512 * 1024))
          .addLast(new FortuneHandler());
    }
}

// FortuneHandler.java
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import org.h2.jdbcx.JdbcConnectionPool;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class FortuneHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    private final ObjectMapper mapper = new ObjectMapper();
    private final JdbcConnectionPool pool;

    public FortuneHandler() {
        pool = JdbcConnectionPool.create("jdbc:h2:mem:fortunes;DB_CLOSE_DELAY=-1", "sa", "");
        initDb();
    }

    private void initDb() {
        try (Connection conn = pool.getConnection()) {
            conn.createStatement().execute(
                "CREATE TABLE IF NOT EXISTS fortunes (id IDENTITY PRIMARY KEY, text VARCHAR(255))");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
        if (request.uri().equals("/fortune") && request.method() == HttpMethod.GET) {
            handleGetFortune(ctx);
        } else if (request.uri().equals("/fortune") && request.method() == HttpMethod.POST) {
            handleAddFortune(ctx, request);
        }
    }

    private void handleGetFortune(ChannelHandlerContext ctx) throws Exception {
        List<String> fortunes = new ArrayList<>();
        try (Connection conn = pool.getConnection()) {
            ResultSet rs = conn.createStatement().executeQuery("SELECT text FROM fortunes");
            while (rs.next()) {
                fortunes.add(rs.getString("text"));
            }
        }

        sendResponse(ctx, HttpResponseStatus.OK, mapper.writeValueAsString(fortunes));
    }

    private void handleAddFortune(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
        Fortune fortune = mapper.readValue(request.content().toString(), Fortune.class);

        try (Connection conn = pool.getConnection()) {
            PreparedStatement stmt = conn.prepareStatement("INSERT INTO fortunes (text) VALUES (?)");
            stmt.setString(1, fortune.getText());
            stmt.execute();
        }

        sendResponse(ctx, HttpResponseStatus.CREATED, "Fortune added");
    }

    private void sendResponse(ChannelHandlerContext ctx, HttpResponseStatus status, String content) {
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status);
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json");
        response.content().writeBytes(content.getBytes());
        ctx.writeAndFlush(response);
    }
}

// Fortune.java
public class Fortune {
    private String text;

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}