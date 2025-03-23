import io.netty.incubator.codec.http3.Http3;
import io.netty.incubator.codec.http3.Http3ServerBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import reactor.netty.http.HttpProtocol;
import reactor.netty.http.server.HttpServer;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.*;
import java.util.*;

// Database Manager Class
class DatabaseManager {
    private static final String DB_URL = "jdbc:h2:mem:fortunes;DB_CLOSE_DELAY=-1";
    private static final String DB_USER = "sa";
    private static final String DB_PASS = "";

    public static void initDatabase() {
        try {
            Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
            Statement stmt = conn.createStatement();

            // Create table
            stmt.execute("CREATE TABLE fortunes (id INT PRIMARY KEY, fortune VARCHAR(255))");

            // Insert sample data
            stmt.execute("INSERT INTO fortunes VALUES (1, 'Fortune favors the bold')");
            stmt.execute("INSERT INTO fortunes VALUES (2, 'The future belongs to those who believe in the beauty of their dreams')");
            stmt.execute("INSERT INTO fortunes VALUES (3, 'Life is what happens while you are busy making other plans')");

            stmt.close();
            conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static List<String> getFortunes() {
        List<String> fortunes = new ArrayList<>();
        try {
            Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT fortune FROM fortunes");

            while (rs.next()) {
                fortunes.add(rs.getString("fortune"));
            }

            rs.close();
            stmt.close();
            conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return fortunes;
    }
}

// Fortune Handler Class
class FortuneHandler extends ChannelInboundHandlerAdapter {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof HttpRequest) {
            HttpRequest request = (HttpRequest) msg;

            if (request.uri().equals("/fortunes")) {
                List<String> fortunes = DatabaseManager.getFortunes();
                String jsonResponse = objectMapper.writeValueAsString(fortunes);

                ByteBuf content = Unpooled.copiedBuffer(jsonResponse, CharsetUtil.UTF_8);
                FullHttpResponse response = new DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1, 
                    HttpResponseStatus.OK,
                    content
                );

                response.headers()
                    .set(HttpHeaderNames.CONTENT_TYPE, "application/json")
                    .set(HttpHeaderNames.CONTENT_LENGTH, content.readableBytes());

                ctx.writeAndFlush(response);
            }
        }
    }
}

// Main Server Class
public class FortuneServer {
    public static void main(String[] args) throws Exception {
        // Initialize database
        DatabaseManager.initDatabase();

        // Create self-signed certificate
        SelfSignedCertificate ssc = new SelfSignedCertificate();
        SslContext sslContext = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey()).build();

        HttpServer.create()
            .protocol(HttpProtocol.H3)
            .secure(spec -> spec.sslContext(sslContext))
            .handle((request, response) -> {
                if (request.uri().equals("/fortunes")) {
                    List<String> fortunes = DatabaseManager.getFortunes();
                    return response.sendString(new ObjectMapper().writeValueAsString(fortunes));
                }
                return response.sendNotFound();
            })
            .bindNow()
            .onDispose()
            .block();
    }
}