import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http3.*;
import io.netty.incubator.codec.quic.*;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import com.h2.jdbcx.JdbcDataSource;
import java.sql.*;
import java.util.*;

public class FortuneServer {
    private final int port;
    private final JdbcDataSource ds;

    public FortuneServer(int port) throws Exception {
        this.port = port;

        // Initialize H2 database
        ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:fortunes;DB_CLOSE_DELAY=-1");
        ds.setUser("sa");
        ds.setPassword("");

        try (Connection conn = ds.getConnection()) {
            conn.createStatement().execute(
                "CREATE TABLE IF NOT EXISTS fortunes (id INT AUTO_INCREMENT, text VARCHAR(255))");
        }
    }

    public void start() throws Exception {
        SelfSignedCertificate ssc = new SelfSignedCertificate();

        QuicServerCodecBuilder serverCodec = QuicServerCodecBuilder.create()
            .sslContext(ssc.cert(), ssc.key())
            .maxIdleTimeout(5000, TimeUnit.MILLISECONDS)
            .initialMaxData(10000000)
            .initialMaxStreamDataBidirectionalLocal(1000000);

        EventLoopGroup group = new NioEventLoopGroup();

        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(group)
             .channel(QuicServerChannel.class)
             .handler(serverCodec.build())
             .childHandler(new ChannelInitializer<SocketChannel>() {
                 @Override
                 protected void initChannel(SocketChannel ch) {
                     ch.pipeline()
                         .addLast(new FortuneServerHandler(ds))
                         .addLast(new HttpResponseEncoder())
                         .addLast(new HttpRequestDecoder());
                 }
             });

            Channel ch = b.bind(port).sync().channel();
            ch.closeFuture().sync();
        } finally {
            group.shutdownGracefully();
        }
    }
}

class FortuneServerHandler extends SimpleChannelInboundHandler<HttpRequest> {
    private final JdbcDataSource ds;

    public FortuneServerHandler(JdbcDataSource ds) {
        this.ds = ds;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, HttpRequest request) throws Exception {
        String uri = request.uri();
        HttpMethod method = request.method();

        if (uri.equals("/fortune") && method == HttpMethod.GET) {
            handleGetFortune(ctx);
        } else if (uri.equals("/fortune") && method == HttpMethod.POST) {
            handleAddFortune(ctx, request);
        } else {
            sendError(ctx, HttpResponseStatus.NOT_FOUND);
        }
    }

    private void handleGetFortune(ChannelHandlerContext ctx) throws SQLException {
        try (Connection conn = ds.getConnection()) {
            ResultSet rs = conn.createStatement()
                .executeQuery("SELECT text FROM fortunes ORDER BY RAND() LIMIT 1");

            String fortune = rs.next() ? rs.getString("text") : "No fortunes available";
            sendResponse(ctx, HttpResponseStatus.OK, fortune);
        }
    }

    private void handleAddFortune(ChannelHandlerContext ctx, HttpRequest request) throws SQLException {
        if (request instanceof FullHttpRequest) {
            FullHttpRequest fullRequest = (FullHttpRequest) request;
            String fortune = fullRequest.content().toString(CharsetUtil.UTF_8);

            try (Connection conn = ds.getConnection()) {
                PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO fortunes (text) VALUES (?)");
                ps.setString(1, fortune);
                ps.executeUpdate();

                sendResponse(ctx, HttpResponseStatus.CREATED, "Fortune added");
            }
        }
    }

    private void sendResponse(ChannelHandlerContext ctx, HttpResponseStatus status, String content) {
        FullHttpResponse response = new DefaultFullHttpResponse(
            HttpVersion.HTTP_1_1,
            status,
            Unpooled.copiedBuffer(content, CharsetUtil.UTF_8)
        );

        response.headers()
            .set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8")
            .set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());

        ctx.writeAndFlush(response);
    }

    private void sendError(ChannelHandlerContext ctx, HttpResponseStatus status) {
        sendResponse(ctx, status, "Error: " + status.toString());
    }
}

class FortuneServerFuture {
    private static final ChannelFuture future = new DefaultChannelFuture(null, true);

    public static ChannelFuture getFuture() {
        return future; 
    }

    public static void setSuccess() {
        future.setSuccess();
    }

    public static void setFailure(Throwable cause) {
        future.setFailure(cause);
    }
}