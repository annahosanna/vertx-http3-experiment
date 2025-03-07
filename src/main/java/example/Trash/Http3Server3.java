// Example 1: Basic HTTP/3 server with a simple route
Vertx vertx = Vertx.vertx(new VertxOptions()
    .setPreferHttp3(true));

Router router = Router.router(vertx);

router.get("/hello").handler(ctx -> {
    ctx.response()
        .putHeader("content-type", "text/plain")
        .end("Hello from HTTP/3!");
});

vertx.createHttpServer(new HttpServerOptions()
        .setUseAlpn(true)
        .setSsl(true)
        .setKeyCertOptions(new PemKeyCertOptions()
            .setKeyPath("server-key.pem")
            .setCertPath("server-cert.pem")))
    .requestHandler(router)
    .listen(8443);

// Example 2: HTTP/3 server with multiple routes and error handling
Router router2 = Router.router(vertx);

router2.route().failureHandler(ctx -> {
    ctx.response()
        .setStatusCode(500)
        .end("Something went wrong");
});

router2.get("/api/users").handler(ctx -> {
    ctx.response()
        .putHeader("content-type", "application/json")
        .end("{\"users\": []}");
});

router2.post("/api/users").handler(ctx -> {
    ctx.response()
        .setStatusCode(201)
        .end();
});

vertx.createHttpServer(new HttpServerOptions()
        .setUseAlpn(true) 
        .setSsl(true)
        .setKeyCertOptions(new PemKeyCertOptions()
            .setKeyPath("server-key.pem")
            .setCertPath("server-cert.pem")))
    .requestHandler(router2)
    .listen(8443);

// Example 3: HTTP/3 server with subrouters and middleware
Router mainRouter = Router.router(vertx);
Router apiRouter = Router.router(vertx);

mainRouter.route().handler(ctx -> {
    ctx.response().putHeader("X-Custom-Header", "value");
    ctx.next();
});

apiRouter.get("/products").handler(ctx -> {
    ctx.response()
        .putHeader("content-type", "application/json")
        .end("[{\"id\": 1, \"name\": \"Product\"}]");
});

mainRouter.mountSubRouter("/api", apiRouter);

vertx.createHttpServer(new HttpServerOptions()
        .setUseAlpn(true)
        .setSsl(true)
        .setKeyCertOptions(new PemKeyCertOptions()
            .setKeyPath("server-key.pem")
            .setCertPath("server-cert.pem")))
    .requestHandler(mainRouter)
    .listen(8443);