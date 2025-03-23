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
    </properties>

    <dependencies>
        <dependency>
            <groupId>io.netty.incubator</groupId>
            <artifactId>netty-incubator-codec-http3</artifactId>
            <version>0.0.20.Final</version>
        </dependency>
        <dependency>
            <groupId>com.h2database</groupId>
            <artifactId>h2</artifactId>
            <version>2.1.214</version>
        </dependency>
    </dependencies>
</project>

// FortuneServer.java
import io.netty.bootstrap.ServerBootstrap; 
import io.netty.channel.*;
import io.netty.incubator.codec.http3.*;
import io.netty.handler.ssl.*;
import java.security.cert.CertificateException;

public class FortuneServer {
    private final int port;
    private final DatabaseService dbService;

    public FortuneServer(int port) {
        this.port = port;
        this.dbService = new DatabaseService();
    }

    public void start() throws CertificateException, InterruptedException {
        QuicServerCodecBuilder quicServerCodecBuilder = new QuicServerCodecBuilder()
            .certificateChain("cert.pem")
            .privateKey("key.pem")
            .applicationProtocols("h3")
            .maxIdleTimeout(5000, TimeUnit.MILLISECONDS)
            .initialMaxData(10000000)
            .initialMaxStreamDataBidirectionalLocal(1000000)
            .initialMaxStreamDataBidirectionalRemote(1000000)
            .initialMaxStreamsBidirectional(100);

        ChannelFuture future = quicServerCodecBuilder.build()
            .handler(new FortuneServerHandler(dbService))
            .bind(port)
            .sync();

        future.channel().closeFuture().sync();
    }
}

// FortuneServerHandler.java
public class FortuneServerHandler extends ChannelInboundHandlerAdapter {
    private final DatabaseService dbService;

    public FortuneServerHandler(DatabaseService dbService) {
        this.dbService = dbService;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof Http3HeadersFrame) {
            Http3HeadersFrame headersFrame = (Http3HeadersFrame) msg;
            String path = headersFrame.headers().get(":path");
            String method = headersFrame.headers().get(":method");

            if ("/fortune".equals(path) && "GET".equals(method)) {
                handleGetFortune(ctx);
            } else if ("/fortune".equals(path) && "POST".equals(method)) {
                handleAddFortune(ctx);
            }
        }
    }

    private void handleGetFortune(ChannelHandlerContext ctx) {
        String fortune = dbService.getRandomFortune();
        sendResponse(ctx, fortune, 200);
    }

    private void handleAddFortune(ChannelHandlerContext ctx) {
        // Implementation for adding fortune
        sendResponse(ctx, "Fortune added successfully", 201);
    }

    private void sendResponse(ChannelHandlerContext ctx, String content, int status) {
        Http3Headers headers = new DefaultHttp3Headers()
            .status(String.valueOf(status))
            .add("content-type", "text/plain");

        ctx.write(new DefaultHttp3HeadersFrame(headers));
        ctx.write(new DefaultHttp3DataFrame(Unpooled.copiedBuffer(content, CharsetUtil.UTF_8)));
        ctx.flush();
    }
}

// DatabaseService.java
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class DatabaseService {
    private static final String DB_URL = "jdbc:h2:./fortunes";
    private static final String DB_USER = "sa";
    private static final String DB_PASSWORD = "";

    public DatabaseService() {
        initDatabase();
    }

    private void initDatabase() {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            Statement stmt = conn.createStatement();
            stmt.execute("CREATE TABLE IF NOT EXISTS fortunes (id INT AUTO_INCREMENT, text VARCHAR(255))");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public String getRandomFortune() {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT text FROM fortunes ORDER BY RAND() LIMIT 1");
            if (rs.next()) {
                return rs.getString("text");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "No fortune available";
    }

    public void addFortune(String fortune) {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            PreparedStatement pstmt = conn.prepareStatement("INSERT INTO fortunes (text) VALUES (?)");
            pstmt.setString(1, fortune);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}

// Main.java
public class Main {
    public static void main(String[] args) throws Exception {
        FortuneServer server = new FortuneServer(8443);
        server.start();
    }
}