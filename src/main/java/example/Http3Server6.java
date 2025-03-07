// Fortune Server Example 1
public class FortuneServerOne extends AbstractVerticle {
    private static final List<String> FORTUNES = Arrays.asList(
        "A beautiful, smart, and loving person will be coming into your life.",
        "A dubious friend may be an enemy in camouflage.",
        "A faithful friend is a strong defense."
    );

    @Override
    public void start() {
        HttpServerOptions options = new HttpServerOptions()
            .setUseAlpn(true)
            .setVersion(HttpVersion.HTTP_3);

        vertx.createHttpServer(options)
            .requestHandler(router -> {
                router.get("/fortune").handler(ctx -> {
                    String randomFortune = FORTUNES.get(new Random().nextInt(FORTUNES.size()));
                    JsonObject response = new JsonObject().put("fortune", randomFortune);
                    ctx.response()
                        .putHeader("content-type", "application/json")
                        .end(response.encode());
                });
            })
            .listen(8080);
    }
}

// Fortune Server Example 2 
public class FortuneServerTwo extends AbstractVerticle {
    private final Map<Integer, String> fortuneMap = new HashMap<>();

    @Override
    public void start() {
        initializeFortunes();

        Router router = Router.router(vertx);
        router.get("/api/fortune").handler(this::handleFortune);

        HttpServerOptions options = new HttpServerOptions()
            .setUseAlpn(true)
            .setVersion(HttpVersion.HTTP_3);

        vertx.createHttpServer(options)
            .requestHandler(router)
            .listen(8081);
    }

    private void handleFortune(RoutingContext ctx) {
        int randomKey = new Random().nextInt(fortuneMap.size()) + 1;
        JsonObject fortune = new JsonObject()
            .put("id", randomKey)
            .put("message", fortuneMap.get(randomKey));

        ctx.response()
            .putHeader("content-type", "application/json")
            .end(fortune.encode());
    }

    private void initializeFortunes() {
        fortuneMap.put(1, "Your road to glory will be rocky but fulfilling.");
        fortuneMap.put(2, "Patience is virtue, tolerance is strength.");
        fortuneMap.put(3, "The greatest adventure is the one that lies ahead.");
    }
}

// Fortune Server Example 3
public class FortuneServerThree extends AbstractVerticle {
    private final JsonArray fortunes = new JsonArray()
        .add("Change is happening in your life, so go with the flow!")
        .add("Don't just think, act!")
        .add("Every wise man started out by asking many questions.");

    @Override
    public void start(Promise<Void> startPromise) {
        Router router = Router.router(vertx);

        router.get("/fortunes")
            .handler(ctx -> {
                int index = new Random().nextInt(fortunes.size());
                JsonObject response = new JsonObject()
                    .put("timestamp", System.currentTimeMillis())
                    .put("fortune", fortunes.getString(index));

                ctx.response()
                    .putHeader("content-type", "application/json")
                    .end(response.encode());
            });

        HttpServerOptions options = new HttpServerOptions()
            .setUseAlpn(true)
            .setVersion(HttpVersion.HTTP_3);

        vertx.createHttpServer(options)
            .requestHandler(router)
            .listen(8082, http -> {
                if (http.succeeded()) {
                    startPromise.complete();
                } else {
                    startPromise.fail(http.cause());
                }
            });
    }
}