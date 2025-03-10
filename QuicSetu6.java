import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.incubator.codec.http3.Http3;
import io.netty.incubator.codec.http3.QuicServerCodecBuilder;
import io.netty.incubator.codec.quic.*;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import org.h2.jdbcx.JdbcConnectionPool;

import java.net.InetSocketAddress;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class FortuneServer {
    private static final int PORT = 8443;
    private final Vertx vertx;
    private final FortuneHandler fortuneHandler;
    private final JdbcConnectionPool connectionPool;

    public FortuneServer() {
        this.vertx = Vertx.vertx();
        this.connectionPool = JdbcConnectionPool.create(
                "jdbc:h2:mem:fortunes;DB_CLOSE_DELAY=-1",
                "sa",
                "");
        this.fortuneHandler = new FortuneHandler(connectionPool);
        initDatabase();
    }

    private void initDatabase() {
        try (Connection conn = connectionPool.getConnection()) {
            conn.createStatement().execute(
                "CREATE TABLE IF NOT EXISTS fortunes (id INT AUTO_INCREMENT, text VARCHAR(255))");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void start() throws Exception {
        QuicServerCodecBuilder.create()
            .certificateChain("path/to/cert.pem")
            .privateKey("path/to/key.pem")
            .handler(new ChannelInitializer<Channel>() {
                @Override
                protected void initChannel(Channel ch) {
                    ch.pipeline().addLast(fortuneHandler);
                }
            })
            .bind(new InetSocketAddress(PORT))
            .sync()
            .channel()
            .closeFuture()
            .await(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
    }

    public static void main(String[] args) throws Exception {
        new FortuneServer().start();
    }
}

@ChannelHandler.Sharable
class FortuneHandler extends Http3.HttpHandler {
    private final JdbcConnectionPool connectionPool;
    private final FortuneFutures fortuneFutures;

    public FortuneHandler(JdbcConnectionPool connectionPool) {
        this.connectionPool = connectionPool;
        this.fortuneFutures = new FortuneFutures(connectionPool);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Http3.HttpRequest request) {
        if (request.method().equals(HttpMethod.GET) && request.uri().equals("/fortune")) {
            fortuneFutures.getRandomFortune()
                .onSuccess(fortune -> sendResponse(ctx, 200, fortune))
                .onFailure(err -> sendResponse(ctx, 500, err.getMessage()));
        } else if (request.method().equals(HttpMethod.POST) && request.uri().equals("/fortune")) {
            String fortune = request.content().toString();
            fortuneFutures.addFortune(fortune)
                .onSuccess(v -> sendResponse(ctx, 201, "Fortune added"))
                .onFailure(err -> sendResponse(ctx, 500, err.getMessage()));
        } else {
            sendResponse(ctx, 404, "Not Found");
        }
    }

    private void sendResponse(ChannelHandlerContext ctx, int status, String content) {
        Http3.HttpResponse response = new Http3.HttpResponse()
            .status(status)
            .content(content);
        ctx.writeAndFlush(response);
    }
}

class FortuneFutures {
    private final JdbcConnectionPool connectionPool;

    public FortuneFutures(JdbcConnectionPool connectionPool) {
        this.connectionPool = connectionPool;
    }

    public Future<String> getRandomFortune() {
        return Future.future(promise -> {
            try (Connection conn = connectionPool.getConnection()) {
                PreparedStatement stmt = conn.prepareStatement(
                    "SELECT text FROM fortunes ORDER BY RAND() LIMIT 1");
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    promise.complete(rs.getString("text"));
                } else {
                    promise.complete("No fortunes available");
                }
            } catch (Exception e) {
                promise.fail(e);
            }
        });
    }

    public Future<Void> addFortune(String fortune) {
        return Future.future(promise -> {
            try (Connection conn = connectionPool.getConnection()) {
                PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO fortunes (text) VALUES (?)");
                stmt.setString(1, fortune);
                stmt.executeUpdate();
                promise.complete();
            } catch (Exception e) {
                promise.fail(e);
            }
        });
    }
}