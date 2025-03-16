<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.example</groupId>
    <artifactId>http3-fortune-server</artifactId>
    <version>1.0-SNAPSHOT</version>

    <properties>
        <maven.compiler.source>17</maven.compiler.source>
        <maven.compiler.target>17</maven.compiler.target>
        <netty.version>4.1.100.Final</netty.version>
        <netty.incubator.version>0.0.21.Final</netty.incubator.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>io.netty.incubator</groupId>
            <artifactId>netty-incubator-codec-http3</artifactId>
            <version>${netty.incubator.version}</version>
        </dependency>
        <dependency>
            <groupId>com.h2database</groupId>
            <artifactId>h2</artifactId>
            <version>2.2.224</version>
        </dependency>
    </dependencies>
</project>

// FortuneServer.java
import io.netty.bootstrap.ServerBootstrap; 
import io.netty.incubator.codec.http3.*;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;

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
             .channel(Http3ServerChannel.class)
             .childHandler(new FortuneServerInitializer());

            Channel ch = b.bind(port).sync().channel();
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

// FortuneServerInitializer.java
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;

public class FortuneServerInitializer extends ChannelInitializer<SocketChannel> {
    @Override
    public void initChannel(SocketChannel ch) {
        ChannelPipeline pipeline = ch.pipeline();
        pipeline.addLast(new Http3ServerConnectionHandler());
        pipeline.addLast(new FortuneHandler());
    }
}

// FortuneHandler.java
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import java.sql.*;

public class FortuneHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    private final DatabaseManager dbManager;

    public FortuneHandler() {
        this.dbManager = new DatabaseManager();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) {
        if (request.method() == HttpMethod.POST && request.uri().equals("/fortune")) {
            String fortune = request.content().toString(CharsetUtil.UTF_8);
            dbManager.addFortune(fortune);
            sendResponse(ctx, HttpResponseStatus.CREATED, "Fortune added");
        } else if (request.method() == HttpMethod.GET && request.uri().equals("/fortune")) {
            String fortune = dbManager.getRandomFortune();
            sendResponse(ctx, HttpResponseStatus.OK, fortune);
        } else {
            sendResponse(ctx, HttpResponseStatus.NOT_FOUND, "Not Found");
        }
    }

    private void sendResponse(ChannelHandlerContext ctx, HttpResponseStatus status, String content) {
        FullHttpResponse response = new DefaultFullHttpResponse(
            HttpVersion.HTTP_1_1, 
            status,
            Unpooled.copiedBuffer(content, CharsetUtil.UTF_8)
        );
        ctx.writeAndFlush(response);
    }
}

// DatabaseManager.java
import java.sql.*;

public class DatabaseManager {
    private static final String DB_URL = "jdbc:h2:mem:fortunes;DB_CLOSE_DELAY=-1";

    public DatabaseManager() {
        initDatabase();
    }

    private void initDatabase() {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS fortunes (" +
                "id IDENTITY PRIMARY KEY," +
                "text VARCHAR(255) NOT NULL" +
                ")"
            );
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize database", e);
        }
    }

    public void addFortune(String fortune) {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(
                "INSERT INTO fortunes (text) VALUES (?)"
             )) {
            pstmt.setString(1, fortune);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to add fortune", e);
        }
    }

    public String getRandomFortune() {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                "SELECT text FROM fortunes ORDER BY RAND() LIMIT 1"
             )) {
            if (rs.next()) {
                return rs.getString("text");
            }
            return "No fortunes available";
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get fortune", e);
        }
    }
}