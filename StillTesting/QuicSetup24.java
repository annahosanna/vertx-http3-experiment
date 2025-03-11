// Main.java
import io.netty.incubator.codec.http3.QuicStreamChannel;
import io.netty.incubator.quic.QuicChannel;
import io.netty.incubator.quic.QuicServer;
import io.netty.incubator.quic.QuicServerBuilder;
import javax.net.ssl.SSLContext;
import java.security.cert.X509Certificate;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

public class Main {
    public static void main(String[] args) throws Exception {
        // Generate self-signed certificate
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();

        X500Name issuerName = new X500Name("CN=localhost");
        ContentSigner contentSigner = new JcaContentSignerBuilder("SHA256withRSA").build(keyPair.getPrivate());

        // Initialize SSL context with self-signed cert
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(keyPair.getPrivate(), new X509Certificate[]{cert}, null);

        // Initialize database
        DatabaseInitializer.init();

        // Create QUIC server
        QuicServer server = QuicServerBuilder.create()
            .sslContext(sslContext)
            .handler(new FortuneServerHandler())
            .build();

        server.bind(8443).sync().channel().closeFuture().sync();
    }
}

// DatabaseInitializer.java
import org.h2.Driver;
import java.sql.*;

public class DatabaseInitializer {
    public static void init() throws SQLException {
        Connection conn = DriverManager.getConnection("jdbc:h2:mem:fortunes");
        Statement stmt = conn.createStatement();

        stmt.execute("CREATE TABLE fortunes (id INT PRIMARY KEY, text VARCHAR(255))");
        stmt.execute("INSERT INTO fortunes VALUES (1, 'Fortune favors the bold')");
        stmt.execute("INSERT INTO fortunes VALUES (2, 'A journey of a thousand miles begins with a single step')");
        stmt.execute("INSERT INTO fortunes VALUES (3, 'The best time to plant a tree was 20 years ago. The second best time is now')");

        stmt.close();
        conn.close();
    }
}

// FortuneServerHandler.java
import io.netty.incubator.codec.http3.Http3HeadersFrame;
import io.netty.incubator.codec.http3.Http3DataFrame;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import com.fasterxml.jackson.databind.ObjectMapper;

public class FortuneServerHandler extends ChannelInboundHandlerAdapter {
    private final FortuneService fortuneService = new FortuneService();
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof Http3HeadersFrame) {
            Http3HeadersFrame headersFrame = (Http3HeadersFrame) msg;

            Fortune fortune = fortuneService.getRandomFortune();
            String jsonFortune = mapper.writeValueAsString(fortune);

            Http3HeadersFrame responseHeaders = new DefaultHttp3HeadersFrame();
            responseHeaders.headers()
                .status("200")
                .add("content-type", "application/json");

            ctx.write(responseHeaders);
            ctx.writeAndFlush(new DefaultHttp3DataFrame(Unpooled.copiedBuffer(jsonFortune, CharsetUtil.UTF_8)));
        }
    }
}

// FortuneService.java
import java.sql.*;

public class FortuneService {
    public Fortune getRandomFortune() throws SQLException {
        Connection conn = DriverManager.getConnection("jdbc:h2:mem:fortunes");
        Statement stmt = conn.createStatement();

        ResultSet rs = stmt.executeQuery("SELECT * FROM fortunes ORDER BY RAND() LIMIT 1");
        Fortune fortune = null;
        if (rs.next()) {
            fortune = new Fortune(rs.getInt("id"), rs.getString("text"));
        }

        rs.close();
        stmt.close();
        conn.close();

        return fortune;
    }
}

// Fortune.java
public class Fortune {
    private int id;
    private String text;

    public Fortune(int id, String text) {
        this.id = id;
        this.text = text;
    }

    public int getId() { return id; }
    public String getText() { return text; }
    public void setId(int id) { this.id = id; }
    public void setText(String text) { this.text = text; }
}