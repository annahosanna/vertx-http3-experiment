// Main.java
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http3.Http3ServerConnectionHandler;
import io.netty.incubator.codec.http3.QuicServerCodecBuilder;
import io.netty.incubator.codec.quic.QuicServerBuilder;

public class Main {
    public static void main(String[] args) throws Exception {
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            QuicServerBuilder quicServer = QuicServerCodecBuilder.create()
                .handler(new Http3ServerInitializer())
                .certificateChain("cert.pem")
                .privateKey("key.pem")
                .applicationProtocols("h3");

            quicServer.bind(8443).sync().channel().closeFuture().sync();
        } finally {
            group.shutdownGracefully();
        }
    }
}

// Http3ServerInitializer.java
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.incubator.codec.http3.Http3ServerConnectionHandler;

public class Http3ServerInitializer extends ChannelInitializer<SocketChannel> {
    @Override
    protected void initChannel(SocketChannel ch) {
        ChannelPipeline p = ch.pipeline();
        p.addLast(new Http3ServerConnectionHandler());
        p.addLast(new FortuneHandler());
    }
}

// FortuneHandler.java
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import java.sql.*;

public class FortuneHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    private final H2Database db = new H2Database();

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest req) {
        if (req.method() == HttpMethod.POST && req.uri().equals("/fortune")) {
            String fortune = req.content().toString(CharsetUtil.UTF_8);
            db.addFortune(fortune);
            sendResponse(ctx, HttpResponseStatus.CREATED, "Fortune added");
        } else if (req.method() == HttpMethod.GET && req.uri().equals("/fortune")) {
            String fortune = db.getRandomFortune();
            sendResponse(ctx, HttpResponseStatus.OK, fortune);
        } else {
            sendResponse(ctx, HttpResponseStatus.NOT_FOUND, "Not Found");
        }
    }

    private void sendResponse(ChannelHandlerContext ctx, HttpResponseStatus status, String content) {
        FullHttpResponse response = new DefaultFullHttpResponse(
            HttpVersion.HTTP_1_1, 
            status,
            Unpooled.copiedBuffer(content, CharsetUtil.UTF_8));
        ctx.writeAndFlush(response);
    }
}

// H2Database.java
import java.sql.*;

public class H2Database {
    private static final String URL = "jdbc:h2:./fortune_db";

    public H2Database() {
        try {
            Class.forName("org.h2.Driver");
            try (Connection conn = DriverManager.getConnection(URL)) {
                Statement stmt = conn.createStatement();
                stmt.execute("CREATE TABLE IF NOT EXISTS fortunes (id INT AUTO_INCREMENT, text VARCHAR(255))");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void addFortune(String fortune) {
        try (Connection conn = DriverManager.getConnection(URL)) {
            PreparedStatement ps = conn.prepareStatement("INSERT INTO fortunes (text) VALUES (?)");
            ps.setString(1, fortune);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public String getRandomFortune() {
        try (Connection conn = DriverManager.getConnection(URL)) {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT text FROM fortunes ORDER BY RAND() LIMIT 1");
            if (rs.next()) {
                return rs.getString("text");
            }
            return "No fortunes available";
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}