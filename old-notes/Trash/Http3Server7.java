public class VertxFortuneServerExample2 {

  public static void main(String[] args) {
    Vertx vertx = Vertx.vertx();

    HttpServerOptions http3Options = new HttpServerOptions()
      .setUseAlpn(true)
      .setProtocolVersion(HttpVersion.HTTP_3);

    HttpServerOptions http2Options = new HttpServerOptions()
      .setUseAlpn(true)
      .setProtocolVersion(HttpVersion.HTTP_2);

    HttpServer http3Server = vertx.createHttpServer(http3Options);
    HttpServer http2Server = vertx.createHttpServer(http2Options);

    Router router = Router.router(vertx);

    router
      .get("/fortune")
      .handler(ctx -> {
        JsonObject fortune = new JsonObject()
          .put("fortune", "Adventure awaits around the corner");
        ctx
          .response()
          .putHeader("content-type", "application/json")
          .putHeader("Alt-Svc", "h3=\":8443\"")
          .end(fortune.encode());
      });

    http3Server.requestHandler(router).listen(8443);
    http2Server.requestHandler(router).listen(8444);
  }
}

public class VertxFortuneServerExample3 {

  public static void main(String[] args) {
    Vertx vertx = Vertx.vertx();

    HttpServerOptions http3Options = new HttpServerOptions()
      .setUseAlpn(true)
      .setProtocolVersion(HttpVersion.HTTP_3)
      .setSsl(true)
      .setPemKeyCertOptions(
        new PemKeyCertOptions()
          .setKeyPath("server-key.pem")
          .setCertPath("server-cert.pem")
      );

    HttpServerOptions http2Options = new HttpServerOptions()
      .setUseAlpn(true)
      .setProtocolVersion(HttpVersion.HTTP_2)
      .setSsl(true)
      .setPemKeyCertOptions(
        new PemKeyCertOptions()
          .setKeyPath("server-key.pem")
          .setCertPath("server-cert.pem")
      );

    HttpServer http3Server = vertx.createHttpServer(http3Options);
    HttpServer http2Server = vertx.createHttpServer(http2Options);

    Router router = Router.router(vertx);

    List<String> fortunes = Arrays.asList(
      "A beautiful day awaits you",
      "Your hard work will pay off soon",
      "Good news will come from far away"
    );

    Random random = new Random();

    router
      .get("/fortune")
      .handler(ctx -> {
        String randomFortune = fortunes.get(random.nextInt(fortunes.size()));
        JsonObject fortune = new JsonObject()
          .put("fortune", randomFortune)
          .put("timestamp", System.currentTimeMillis());

        ctx
          .response()
          .putHeader("content-type", "application/json")
          .putHeader("Alt-Svc", "h3=\":8443\"")
          .end(fortune.encode());
      });

    http3Server.requestHandler(router).listen(8443);
    http2Server.requestHandler(router).listen(8444);
  }
}
