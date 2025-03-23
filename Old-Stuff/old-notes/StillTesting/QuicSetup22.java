// DatabaseService.java
import org.h2.jdbcx.JdbcConnectionPool;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseService {
    private final JdbcConnectionPool pool;

    public DatabaseService() {
        pool = JdbcConnectionPool.create("jdbc:h2:mem:fortunes", "sa", "");
        initDatabase();
    }

    private void initDatabase() {
        try (Connection conn = pool.getConnection()) {
            conn.createStatement().execute(
                "CREATE TABLE IF NOT EXISTS fortunes (id INT PRIMARY KEY, text VARCHAR(255))");

            PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO fortunes (id, text) VALUES (?, ?)");

            String[] initialFortunes = {
                "Today is your lucky day!",
                "Good fortune will come to you",
                "A pleasant surprise is waiting for you"
            };

            for (int i = 0; i < initialFortunes.length; i++) {
                ps.setInt(1, i+1);
                ps.setString(2, initialFortunes[i]);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public String getRandomFortune() {
        try (Connection conn = pool.getConnection()) {
            ResultSet rs = conn.createStatement().executeQuery(
                "SELECT text FROM fortunes ORDER BY RAND() LIMIT 1");
            return rs.next() ? rs.getString(1) : "No fortune found";
        } catch (SQLException e) {
            return "Error retrieving fortune";
        }
    }
}

// FortuneHandler.java
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.incubator.codec.http3.Http3HeadersFrame;
import io.netty.incubator.codec.http3.Http3DataFrame;

public class FortuneHandler extends SimpleChannelInboundHandler<Http3HeadersFrame> {
    private final DatabaseService databaseService;

    public FortuneHandler(DatabaseService databaseService) {
        this.databaseService = databaseService;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Http3HeadersFrame frame) {
        String fortune = databaseService.getRandomFortune();
        byte[] content = fortune.getBytes();

        Http3HeadersFrame responseHeaders = new DefaultHttp3HeadersFrame();
        responseHeaders.headers()
            .status("200")
            .add("content-type", "text/plain")
            .add("content-length", String.valueOf(content.length));

        ctx.write(responseHeaders);
        ctx.writeAndFlush(new DefaultHttp3DataFrame(Unpooled.wrappedBuffer(content)));
    }
}

// FortuneServer.java
import io.netty.incubator.codec.http3.Http3;
import reactor.netty.http.HttpProtocol;
import reactor.netty.http.server.HttpServer;
import reactor.netty.incubator.quic.QuicServer;

public class FortuneServer {
    private final DatabaseService databaseService;
    private final int port;

    public FortuneServer(int port) {
        this.databaseService = new DatabaseService();
        this.port = port;
    }

    public void start() {
        QuicServer.create()
            .port(port)
            .protocol(HttpProtocol.H3)
            .handle((req, res) -> {
                FortuneHandler handler = new FortuneHandler(databaseService);
                return res.sendString(handler.handle(req));
            })
            .bindNow()
            .onDispose()
            .block();
    }

    public static void main(String[] args) {
        new FortuneServer(8443).start();
    }
}