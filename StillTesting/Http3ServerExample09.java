import io.netty.incubator.codec.http3.Http3;
import io.netty.incubator.codec.http3.Http3ServerBuilder;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

// Main Server Class
public class Http3FortuneServer extends AbstractVerticle {
    private Router router;
    private Connection dbConnection;

    @Override
    public void start() {
        initDatabase();
        setupRouter();

        Http3ServerBuilder.create()
            .handler(new Http3RequestHandler(router))
            .bind(8443)
            .addListener(future -> {
                if (future.isSuccess()) {
                    System.out.println("Server started on port 8443");
                } else {
                    future.cause().printStackTrace();
                }
            });
    }

    private void initDatabase() {
        try {
            dbConnection = DriverManager.getConnection("jdbc:h2:./fortunes");
            dbConnection.createStatement().execute(
                "CREATE TABLE IF NOT EXISTS fortunes (id INT AUTO_INCREMENT, text VARCHAR(255))"
            );
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void setupRouter() {
        router = Router.router(vertx);
        router.route().handler(BodyHandler.create());

        FortuneHandler fortuneHandler = new FortuneHandler(dbConnection);

        router.post("/fortune").handler(ctx -> 
            fortuneHandler.addFortune(ctx.getBodyAsString())
                .onSuccess(v -> ctx.response().setStatusCode(201).end())
                .onFailure(e -> ctx.response().setStatusCode(500).end())
        );

        router.get("/fortune").handler(ctx ->
            fortuneHandler.getFortune()
                .onSuccess(fortune -> ctx.response().end(fortune))
                .onFailure(e -> ctx.response().setStatusCode(500).end())
        );

        router.route().handler(ctx -> ctx.response().setStatusCode(404).end());
    }
}

// Fortune Handler Class
class FortuneHandler {
    private final Connection dbConnection;

    public FortuneHandler(Connection dbConnection) {
        this.dbConnection = dbConnection;
    }

    public Future<Void> addFortune(String fortune) {
        return Future.fromCompletionStage(() -> {
            var stmt = dbConnection.prepareStatement("INSERT INTO fortunes (text) VALUES (?)");
            stmt.setString(1, fortune);
            stmt.execute();
            return null;
        });
    }

    public Future<String> getFortune() {
        return Future.fromCompletionStage(() -> {
            var stmt = dbConnection.prepareStatement("SELECT text FROM fortunes ORDER BY RANDOM() LIMIT 1");
            var rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getString("text");
            }
            return "No fortunes available";
        });
    }
}

// Http3 Request Handler Class
class Http3RequestHandler implements io.netty.channel.ChannelHandler {
    private final Router router;

    public Http3RequestHandler(Router router) {
        this.router = router;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof Http3HeadersFrame) {
            Http3HeadersFrame headersFrame = (Http3HeadersFrame) msg;
            router.handle(new VertxHttpServerRequest(headersFrame, ctx));
        }
    }
}

// POM.xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.example</groupId>
    <artifactId>http3-fortune-server</artifactId>
    <version>1.0-SNAPSHOT</version>

    <properties>
        <vertx.version>4.3.0</vertx.version>
        <netty.version>4.1.74.Final</netty.version>
        <maven.compiler.source>11</maven.compiler.source>
        <maven.compiler.target>11</maven.compiler.target>
    </properties>

    <dependencies>
        <dependency>
            <groupId>io.vertx</groupId>
            <artifactId>vertx-core</artifactId>
            <version>${vertx.version}</version>
        </dependency>
        <dependency>
            <groupId>io.vertx</groupId>
            <artifactId>vertx-web</artifactId>
            <version>${vertx.version}</version>
        </dependency>
        <dependency>
            <groupId>io.netty.incubator</groupId>
            <artifactId>netty-incubator-codec-http3</artifactId>
            <version>0.0.14.Final</version>
        </dependency>
        <dependency>
            <groupId>com.h2database</groupId>
            <artifactId>h2</artifactId>
            <version>2.1.210</version>
        </dependency>
    </dependencies>
</project>