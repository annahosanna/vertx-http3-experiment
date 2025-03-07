// Example 1: Basic HTTP3 Server with Router
public class BasicHttp3Server {
    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx(new VertxOptions()
            .setQuicOptions(new QuicOptions().setPort(8443)));

        Router router = Router.router(vertx);

        router.get("/").handler(ctx -> {
            ctx.response().end("Hello from HTTP/3 server!");
        });

        vertx.createHttpServer(new HttpServerOptions()
            .setUseAlpn(true)
            .setSsl(true)
            .setSslEngineOptions(new OpenSSLEngineOptions()))
            .requestHandler(router)
            .listen(8443);
    }
}

// Example 2: HTTP3 Server with Multiple Routes
public class MultiRouteHttp3Server {
    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        Router router = Router.router(vertx);

        // Define multiple routes
        router.get("/api/users").handler(ctx -> {
            ctx.response()
                .putHeader("content-type", "application/json")
                .end("[{\"name\":\"John\"},{\"name\":\"Jane\"}]");
        });

        router.post("/api/users").handler(ctx -> {
            ctx.response()
                .setStatusCode(201)
                .end("User created");
        });

        HttpServer server = vertx.createHttpServer(new HttpServerOptions()
            .setUseAlpn(true)
            .setSsl(true)
            .setPort(8443));

        server.requestHandler(router).listen();
    }
}

// Example 3: HTTP3 Server with Subrouters
public class SubrouterHttp3Server {
    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        Router mainRouter = Router.router(vertx);

        // Create subrouter for API endpoints
        Router apiRouter = Router.router(vertx);
        mainRouter.mountSubRouter("/api", apiRouter);

        // Define API routes
        apiRouter.get("/products").handler(ctx -> {
            ctx.response()
                .putHeader("content-type", "application/json")
                .end("{\"products\":[]}");
        });

        apiRouter.get("/orders").handler(ctx -> {
            ctx.response()
                .putHeader("content-type", "application/json")
                .end("{\"orders\":[]}");
        });

        vertx.createHttpServer(new HttpServerOptions()
            .setUseAlpn(true)
            .setSsl(true)
            .setSslEngineOptions(new OpenSSLEngineOptions()))
            .requestHandler(mainRouter)
            .listen(8443);
    }
}