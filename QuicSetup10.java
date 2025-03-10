import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.incubator.codec.http3.*;
import io.netty.incubator.codec.quic.*;
import io.netty.handler.codec.http.*;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import org.h2.jdbcx.JdbcConnectionPool;

import java.security.cert.CertificateException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FortuneServer {
    private final int port;
    private final JdbcConnectionPool connectionPool;

    public FortuneServer(int port) {
        this.port = port;
        this.connectionPool = JdbcConnectionPool.create(
            "jdbc:h2:mem:fortunes;DB_CLOSE_DELAY=-1",
            "sa",
            "password"
        );
        initDb();
    }

    private void initDb() {
        try (Connection conn = connectionPool.getConnection()) {
            conn.createStatement().execute(
                "CREATE TABLE IF NOT EXISTS fortunes (id IDENTITY PRIMARY KEY, text VARCHAR(255))"
            );
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void start() throws CertificateException {
        SelfSignedCertificate ssc = new SelfSignedCertificate();
        QuicSslContext context = QuicSslContextBuilder.forServer(ssc.privateKey(), ssc.certificate())
            .applicationProtocols("h3")
            .build();

        EventLoopGroup group = new NioEventLoopGroup();
        QuicServerCodecBuilder codecBuilder = QuicServerCodecBuilder.create()
            .sslContext(context)
            .maxIdleTimeout(5000, TimeUnit.MILLISECONDS)
            .initialMaxData(10000000)
            .initialMaxStreamDataBidirectionalLocal(1000000)
            .initialMaxStreamsBidirectional(100);

        try {
            ChannelFuture f = new ServerBootstrap()
                .group(group)
                .channel(QuicServerChannel.class)
                .handler(codecBuilder.build())
                .childHandler(new FortuneServerInitializer(connectionPool))
                .bind(port)
                .sync();

            f.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            group.shutdownGracefully();
        }
    }

    public static void main(String[] args) throws CertificateException {
        new FortuneServer(8443).start();
    }
}

class FortuneServerInitializer extends ChannelInitializer<QuicChannel> {
    private final JdbcConnectionPool connectionPool;

    public FortuneServerInitializer(JdbcConnectionPool connectionPool) {
        this.connectionPool = connectionPool;
    }

    @Override
    protected void initChannel(QuicChannel ch) {
        ch.pipeline().addLast(new FortuneHandler(connectionPool));
    }
}

class FortuneHandler extends SimpleChannelInboundHandler<Http3HeadersFrame> {
    private final JdbcConnectionPool connectionPool;

    public FortuneHandler(JdbcConnectionPool connectionPool) {
        this.connectionPool = connectionPool;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Http3HeadersFrame frame) {
        String path = frame.headers().get(":path");
        String method = frame.headers().get(":method");

        if (path.equals("/fortune") && method.equals("GET")) {
            handleGetFortune(ctx);
        } else if (path.equals("/fortune") && method.equals("POST")) {
            handleAddFortune(ctx, frame);
        } else {
            sendResponse(ctx, HttpResponseStatus.NOT_FOUND, "Not Found");
        }
    }

    private void handleGetFortune(ChannelHandlerContext ctx) {
        try (Connection conn = connectionPool.getConnection()) {
            PreparedStatement stmt = conn.prepareStatement("SELECT text FROM fortunes ORDER BY RANDOM() LIMIT 1");
            ResultSet rs = stmt.executeQuery();

            String fortune = rs.next() ? rs.getString("text") : "No fortunes available";
            sendResponse(ctx, HttpResponseStatus.OK, fortune);
        } catch (SQLException e) {
            sendResponse(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, "Database error");
        }
    }

    private void handleAddFortune(ChannelHandlerContext ctx, Http3HeadersFrame frame) {
        if (frame instanceof Http3DataFrame) {
            String fortune = ((Http3DataFrame) frame).content().toString();
            try (Connection conn = connectionPool.getConnection()) {
                PreparedStatement stmt = conn.prepareStatement("INSERT INTO fortunes (text) VALUES (?)");
                stmt.setString(1, fortune);
                stmt.executeUpdate();
                sendResponse(ctx, HttpResponseStatus.CREATED, "Fortune added");
            } catch (SQLException e) {
                sendResponse(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, "Database error");
            }
        }
    }

    private void sendResponse(ChannelHandlerContext ctx, HttpResponseStatus status, String message) {
        Http3Headers headers = new DefaultHttp3Headers()
            .status(status.codeAsText())
            .set(HttpHeaderNames.CONTENT_TYPE, "text/plain");

        ctx.write(new DefaultHttp3HeadersFrame(headers));
        ctx.writeAndFlush(new DefaultHttp3DataFrame(ctx.alloc().buffer().writeBytes(message.getBytes())));
    }
}