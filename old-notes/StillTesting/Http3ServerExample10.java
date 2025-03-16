// Server.java
package com.example.http3server;

import io.netty.incubator.codec.http3.Http3;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;

public class Server {
    private final DatabaseHandler db;
    private final FortuneHandler fortuneHandler;
    private final Router router;

    public Server() {
        this.db = new DatabaseHandler();
        this.fortuneHandler = new FortuneHandler(db);
        this.router = setupRouter();
    }

    private Router setupRouter() {
        Router router = Router.router(Vertx.vertx());

        router.post("/fortune").handler(fortuneHandler::addFortune);
        router.get("/fortune").handler(fortuneHandler::getFortune);
        router.route().handler(ctx -> ctx.response().setStatusCode(404).end());

        return router;
    }

    public void start() {
        Vertx.vertx().createHttpServer()
            .requestHandler(router)
            .listen(8080);
    }
}

// DatabaseHandler.java
package com.example.http3server;

import java.sql.*;
import io.vertx.core.Future;
import io.vertx.core.Promise;

public class DatabaseHandler {
    private final String DB_URL = "jdbc:h2:mem:fortunes;DB_CLOSE_DELAY=-1";

    public DatabaseHandler() {
        initDatabase();
    }

    private void initDatabase() {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            Statement stmt = conn.createStatement();
            stmt.execute("CREATE TABLE IF NOT EXISTS fortunes (id INT AUTO_INCREMENT PRIMARY KEY, text VARCHAR(255))");
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize database", e);
        }
    }

    public Future<Void> addFortune(String fortune) {
        Promise<Void> promise = Promise.promise();
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            PreparedStatement pstmt = conn.prepareStatement("INSERT INTO fortunes (text) VALUES (?)");
            pstmt.setString(1, fortune);
            pstmt.executeUpdate();
            promise.complete();
        } catch (SQLException e) {
            promise.fail(e);
        }
        return promise.future();
    }

    public Future<String> getRandomFortune() {
        Promise<String> promise = Promise.promise();
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT text FROM fortunes ORDER BY RANDOM() LIMIT 1");
            if (rs.next()) {
                promise.complete(rs.getString("text"));
            } else {
                promise.complete("No fortunes available");
            }
        } catch (SQLException e) {
            promise.fail(e);
        }
        return promise.future();
    }
}

// FortuneHandler.java
package com.example.http3server;

import io.vertx.ext.web.RoutingContext;

public class FortuneHandler {
    private final DatabaseHandler db;

    public FortuneHandler(DatabaseHandler db) {
        this.db = db;
    }

    public void addFortune(RoutingContext ctx) {
        String fortune = ctx.getBodyAsJson().getString("fortune");
        db.addFortune(fortune)
            .onSuccess(v -> ctx.response().setStatusCode(201).end())
            .onFailure(err -> ctx.response().setStatusCode(500).end(err.getMessage()));
    }

    public void getFortune(RoutingContext ctx) {
        db.getRandomFortune()
            .onSuccess(fortune -> ctx.response()
                .putHeader("content-type", "application/json")
                .end(String.format("{\"fortune\": \"%s\"}", fortune)))
            .onFailure(err -> ctx.response().setStatusCode(500).end(err.getMessage()));
    }
}

// pom.xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.example</groupId>
    <artifactId>http3-fortune-server</artifactId>
    <version>1.0-SNAPSHOT</version>

    <properties>
        <maven.compiler.source>11</maven.compiler.source>
        <maven.compiler.target>11</maven.compiler.target>
        <vertx.version>4.4.0</vertx.version>
        <netty.incubator.version>0.0.21.Final</netty.incubator.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>io.vertx</groupId>
            <artifactId>vertx-web</artifactId>
            <version>${vertx.version}</version>
        </dependency>
        <dependency>
            <groupId>io.netty.incubator</groupId>
            <artifactId>netty-incubator-codec-http3</artifactId>
            <version>${netty.incubator.version}</version>
        </dependency>
        <dependency>
            <groupId>com.h2database</groupId>
            <artifactId>h2</artifactId>
            <version>2.1.214</version>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.8.1</version>
                <configuration>
                    <source>${maven.compiler.source}</source>
                    <target>${maven.compiler.target}</target>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>