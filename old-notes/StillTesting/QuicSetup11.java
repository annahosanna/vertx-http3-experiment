// FortuneServer.java
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import io.netty.incubator.codec.quic.QuicServerCodecBuilder;
import io.netty.incubator.codec.quic.QuicChannel;
import io.netty.incubator.codec.http3.Http3HeadersFrame;
import io.netty.incubator.codec.http3.Http3DataFrame;

public class FortuneServer {
    private final int port;

    public FortuneServer(int port) {
        this.port = port;
    }

    public void start() throws Exception {
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        try {
            SelfSignedCertificate cert = new SelfSignedCertificate();

            QuicServerCodecBuilder serverCodec = QuicServerCodecBuilder.create()
                .certificate(cert.certificate())
                .privateKey(cert.privateKey());

            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new FortuneServerInitializer(serverCodec))
                .option(ChannelOption.SO_BACKLOG, 128)
                .childOption(ChannelOption.SO_KEEPALIVE, true);

            ChannelFuture f = b.bind(port).sync();
            f.channel().closeFuture().sync();

        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
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
import io.netty.incubator.codec.quic.QuicServerCodecBuilder;

public class FortuneServerInitializer extends ChannelInitializer<SocketChannel> {
    private final QuicServerCodecBuilder serverCodec;

    public FortuneServerInitializer(QuicServerCodecBuilder serverCodec) {
        this.serverCodec = serverCodec;
    }

    @Override
    public void initChannel(SocketChannel ch) {
        ChannelPipeline p = ch.pipeline();
        p.addLast(serverCodec.build());
        p.addLast(new FortuneHandler());
    }
}

// FortuneHandler.java
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.incubator.codec.http3.Http3HeadersFrame;
import io.netty.incubator.codec.http3.Http3DataFrame;
import org.h2.jdbcx.JdbcDataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class FortuneHandler extends SimpleChannelInboundHandler<Http3HeadersFrame> {
    private final JdbcDataSource ds;

    public FortuneHandler() {
        ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:fortunes");
        initDb();
    }

    private void initDb() {
        try (Connection conn = ds.getConnection()) {
            conn.createStatement().execute(
                "CREATE TABLE IF NOT EXISTS fortunes (id INT AUTO_INCREMENT, text VARCHAR(255))");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Http3HeadersFrame frame) {
        String path = frame.headers().get(":path").toString();
        String method = frame.headers().get(":method").toString();

        if (method.equals("GET") && path.equals("/fortune")) {
            sendRandomFortune(ctx);
        } else if (method.equals("POST") && path.equals("/fortune")) {
            ctx.channel().read();
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof Http3DataFrame) {
            addFortune(ctx, ((Http3DataFrame) msg).content().toString());
        }
    }

    private void sendRandomFortune(ChannelHandlerContext ctx) {
        try (Connection conn = ds.getConnection()) {
            ResultSet rs = conn.createStatement()
                .executeQuery("SELECT text FROM fortunes ORDER BY RAND() LIMIT 1");

            if (rs.next()) {
                String fortune = rs.getString("text");
                Http3HeadersFrame response = createResponse(200);
                ctx.write(response);
                ctx.writeAndFlush(new Http3DataFrame(fortune));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void addFortune(ChannelHandlerContext ctx, String fortune) {
        try (Connection conn = ds.getConnection()) {
            PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO fortunes (text) VALUES (?)");
            ps.setString(1, fortune);
            ps.executeUpdate();

            Http3HeadersFrame response = createResponse(201);
            ctx.writeAndFlush(response);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Http3HeadersFrame createResponse(int status) {
        Http3HeadersFrame frame = new Http3HeadersFrame();
        frame.headers()
            .add(":status", Integer.toString(status))
            .add("content-type", "text/plain");
        return frame;
    }
}