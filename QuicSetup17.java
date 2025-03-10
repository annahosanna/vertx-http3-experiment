// QuicServer.java
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.incubator.codec.quic.*;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import java.net.InetSocketAddress;
import java.security.cert.CertificateException;

public class QuicServer {
    public static void main(String[] args) throws CertificateException {
        SelfSignedCertificate cert = new SelfSignedCertificate();
        QuicSslContext sslContext = QuicSslContextBuilder.forServer(cert.privateKey(), cert.certificate())
            .applicationProtocols("h3")
            .build();

        NioEventLoopGroup group = new NioEventLoopGroup(1);
        try {
            Bootstrap bs = new Bootstrap();
            Channel channel = bs.group(group)
                .channel(QuicChannel.class)
                .handler(new QuicServerCodecBuilder()
                    .sslContext(sslContext)
                    .maxIdleTimeout(5000, TimeUnit.MILLISECONDS)
                    .initialMaxData(10000000)
                    .initialMaxStreamDataBidirectionalLocal(1000000)
                    .build())
                .bind(new InetSocketAddress(8080))
                .sync()
                .channel();

            ServerHandler.setupChannel(channel);
            channel.closeFuture().sync();
        } finally {
            group.shutdownGracefully();
        }
    }
}

// ServerHandler.java
import io.netty.channel.*;
import io.netty.handler.codec.http3.*;
import java.sql.*;

public class ServerHandler {
    static void setupChannel(Channel channel) {
        ChannelPipeline pipeline = channel.pipeline();
        pipeline.addLast(new Http3FrameHandler());
        pipeline.addLast(new DatabaseHandler());
    }
}

// Http3FrameHandler.java
import io.netty.channel.*;
import io.netty.handler.codec.http3.*;

public class Http3FrameHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof Http3HeadersFrame) {
            ctx.fireChannelRead(msg);
        }
        ReferenceCountUtil.release(msg);
    }
}

// DatabaseHandler.java
import io.netty.channel.*;
import io.netty.handler.codec.http3.*;
import java.sql.*;

public class DatabaseHandler extends ChannelInboundHandlerAdapter {
    private static final String DB_URL = "jdbc:h2:./h2db";

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof Http3HeadersFrame) {
            Http3HeadersFrame headersFrame = (Http3HeadersFrame) msg;
            saveHeaders(headersFrame);
        }
    }

    private void saveHeaders(Http3HeadersFrame frame) {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            String sql = "INSERT INTO headers (name, value) VALUES (?, ?)";
            frame.headers().forEach(header -> {
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, header.getKey());
                    stmt.setString(2, header.getValue());
                    stmt.executeUpdate();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            });
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}