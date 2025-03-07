package example;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.web.Router;

public class Http3Server2 {

  public static void main(String[] args) {
    Vertx vertx = Vertx.vertx();
    Router router = Router.router(vertx);

    // Add routes
    router
      .get("/")
      .handler(ctx -> {
        ctx
          .response()
          .putHeader("content-type", "text/plain")
          .end("Hello from HTTP/3 server!");
      });

    // Configure HTTP/3 server options
    HttpServerOptions options = new HttpServerOptions()
      .setUseAlpn(true)
      .setInitialSettings(
        new io.vertx.core.http.Http2Settings().setMaxConcurrentStreams(100)
      )
      .setSsl(true)
      .setUseProxyProtocol(true);

    // Create and start the server
    HttpServer server = vertx.createHttpServer(options);

    server
      .requestHandler(router)
      .listen(8443, "0.0.0.0", res -> {
        if (res.succeeded()) {
          System.out.println("HTTP/3 server started on port 8443");
        } else {
          System.err.println("Failed to start server: " + res.cause());
        }
      });
  }
}
