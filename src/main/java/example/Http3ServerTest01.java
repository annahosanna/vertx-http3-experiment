package example;

import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http3.Http3ServerCodec;
// Attempt 17. Might actually work
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.web.Router;

public class Http3ServerTest01 extends AbstractVerticle {

  @Override
  public void start() {
    Router router = Router.router(vertx);

    HttpServerOptions options = new HttpServerOptions()
      .setUseAlpn(true)
      .setSsl(true)
      .setSslHandshakeTimeout(10)
      .setPort(8443);

    HttpServer server = vertx.createHttpServer(options);

    // Add HTTP/3 codec to pipeline
    // No such method as nettyHttpServer
    server
      .nettyHttpServer()
      .pipeline()
      .addLast("http3codec", new Http3ServerCodec());

    // Configure routes
    router
      .route("/api/*")
      .handler(ctx -> {
        ctx
          .response()
          .putHeader("content-type", "application/json")
          .end("{\"message\": \"Hello from HTTP/3!\"}");
      });

    // Start server
    server
      .requestHandler(router)
      .listen(res -> {
        if (res.succeeded()) {
          System.out.println(
            "Server started on port " + res.result().actualPort()
          );
        } else {
          System.out.println("Failed to start server: " + res.cause());
        }
      });
  }

  public static void main(String[] args) {
    Vertx vertx = Vertx.vertx();
    vertx.deployVerticle(new Http3ServerTest01());
  }
}
