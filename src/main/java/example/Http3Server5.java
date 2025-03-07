// Example 1: Basic HTTP/3 Vertx Server
public class BasicHttp3Server {
    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();

        HttpServerOptions options = new HttpServerOptions()
            .setVersion(HttpVersion.HTTP_3)
            .setPort(8443)
            .setHost("localhost");

        vertx.createHttpServer(options)
            .requestHandler(req -> {
                req.response()
                    .putHeader("content-type", "text/plain")
                    .end("Hello from HTTP/3 server!");
            })
            .listen();
    }
}

// Example 2: HTTP/3 Server with SSL/TLS
public class SecureHttp3Server {
    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();

        HttpServerOptions options = new HttpServerOptions()
            .setVersion(HttpVersion.HTTP_3)
            .setSsl(true)
            .setKeyStoreOptions(new JksOptions()
                .setPath("keystore.jks")
                .setPassword("password"))
            .setPort(8443);

        vertx.createHttpServer(options)
            .requestHandler(req -> {
                req.response()
                    .putHeader("content-type", "text/plain")
                    .end("Hello from secure HTTP/3 server!");
            })
            .listen();
    }
}

// Example 3: HTTP/3 Server with Custom Configuration
public class CustomHttp3Server {
    public static void main(String[] args) {
        VertxOptions vertxOptions = new VertxOptions()
            .setPreferNativeTransport(true);
        Vertx vertx = Vertx.vertx(vertxOptions);

        HttpServerOptions options = new HttpServerOptions()
            .setVersion(HttpVersion.HTTP_3)
            .setPort(8443)
            .setIdleTimeout(60)
            .setInitialSettings(new Http2Settings()
                .setMaxConcurrentStreams(100)
                .setInitialWindowSize(65535))
            .setAltSvcMaxAge(3600);

        vertx.createHttpServer(options)
            .requestHandler(req -> {
                req.response()
                    .putHeader("content-type", "text/plain")
                    .end("Hello from customized HTTP/3 server!");
            })
            .listen(ar -> {
                if (ar.succeeded()) {
                    System.out.println("Server started on port " + ar.result().actualPort());
                } else {
                    System.out.println("Failed to start server: " + ar.cause().getMessage());
                }
            });
    }
}