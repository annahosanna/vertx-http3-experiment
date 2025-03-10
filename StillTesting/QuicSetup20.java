import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.incubator.codec.quic.QuicChannel;
import io.netty.incubator.codec.quic.QuicServerCodecBuilder;
import io.netty.incubator.codec.quic.QuicStreamChannel;
import io.netty.handler.ssl.util.SelfSignedCertificate;

public class FortuneServer {
    private final int port;

    public FortuneServer(int port) {
        this.port = port;
    }

    public void start() throws Exception {
        SelfSignedCertificate cert = new SelfSignedCertificate();
        EventLoopGroup group = new NioEventLoopGroup();

        try {
            QuicServerCodecBuilder codecBuilder = new QuicServerCodecBuilder()
                .certificate(cert.certificate())
                .privateKey(cert.privateKey())
                .handler(new FortuneServerInitializer());

            ServerBootstrap b = new ServerBootstrap();
            b.group(group)
             .channel(codecBuilder.buildChannel())
             .childHandler(new ChannelInitializer<Channel>() {
                 @Override
                 protected void initChannel(Channel ch) {
                     ch.pipeline().addLast(codecBuilder.build());
                 }
             })
             .childOption(ChannelOption.AUTO_READ, true);

            Channel ch = b.bind(port).sync().channel();
            ch.closeFuture().sync();
        } finally {
            group.shutdownGracefully();
        }
    }

    public static void main(String[] args) throws Exception {
        int port = 8080;
        new FortuneServer(port).start();
        DatabaseInitializer.initDb();
    }
}

import io.netty.channel.ChannelInitializer;
import io.netty.incubator.codec.quic.QuicStreamChannel;

public class FortuneServerInitializer extends ChannelInitializer<QuicStreamChannel> {
    @Override
    protected void initChannel(QuicStreamChannel ch) {
        ch.pipeline()
          .addLast(new FortuneHandler());
    }
}

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.incubator.codec.http3.Http3DataFrame;
import io.netty.incubator.codec.http3.Http3HeadersFrame;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class FortuneHandler extends SimpleChannelInboundHandler<Http3DataFrame> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Http3DataFrame frame) throws Exception {
        String fortune = getRandomFortune();

        Http3HeadersFrame headers = new DefaultHttp3HeadersFrame();
        headers.headers()
              .status("200")
              .add("content-type", "text/plain");

        ctx.write(headers);

        ByteBuf content = ctx.alloc().buffer();
        content.writeBytes(fortune.getBytes());
        ctx.writeAndFlush(new DefaultHttp3DataFrame(content));
    }

    private String getRandomFortune() throws Exception {
        String fortune = "";
        try (Connection conn = DatabaseConnection.getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(
                "SELECT text FROM fortunes ORDER BY RANDOM() LIMIT 1");
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                fortune = rs.getString("text");
            }
        }
        return fortune;
    }
}

import java.sql.Connection;
import java.sql.DriverManager;

public class DatabaseConnection {
    private static final String URL = "jdbc:h2:mem:fortunes;DB_CLOSE_DELAY=-1";

    public static Connection getConnection() throws Exception {
        return DriverManager.getConnection(URL);
    }
}

import java.sql.Connection;
import java.sql.Statement;

public class DatabaseInitializer {
    public static void initDb() throws Exception {
        try (Connection conn = DatabaseConnection.getConnection()) {
            Statement stmt = conn.createStatement();

            stmt.execute("CREATE TABLE IF NOT EXISTS fortunes (id INT PRIMARY KEY AUTO_INCREMENT, text VARCHAR(255))");

            stmt.execute("INSERT INTO fortunes (text) VALUES ('Fortune favors the bold')");
            stmt.execute("INSERT INTO fortunes (text) VALUES ('A journey of a thousand miles begins with a single step')");
            stmt.execute("INSERT INTO fortunes (text) VALUES ('The best time to plant a tree was 20 years ago. The second best time is now')");
            stmt.execute("INSERT INTO fortunes (text) VALUES ('Be the change you wish to see in the world')");
            stmt.execute("INSERT INTO fortunes (text) VALUES ('Today is the first day of the rest of your life')");
        }
    }
}