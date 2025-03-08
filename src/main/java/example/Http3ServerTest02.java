package example;

import io.netty.incubator.codec.http3.Http3;
import io.netty.incubator.codec.http3.Http3ServerCodec;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.net.JksOptions;

// Attempt 12. Might work
public class Http3VertxServer extends AbstractVerticle {

  @Override
  public void start() throws Exception {
    // Configure HTTP/3 options
    HttpServerOptions options = new HttpServerOptions()
      .setUseAlpn(true)
      .setSsl(true)
      .setSslEngineOptions(
        new JksOptions().setPath("server-keystore.jks").setPassword("password")
      )
      .setPort(443);

    // Create and configure Vertx HTTP server
    HttpServer server = vertx.createHttpServer(options);

    // Add HTTP/3 codec
    // connectionHandler method exists and returns a HttpConnection
    // But there is no such thing as a connectionHandler
    server.connectionHandler(conn -> {
      conn.channelHandlerContext().pipeline().addLast(new Http3ServerCodec());
    });

    // Handle requests
    server.requestHandler(req -> {
      req
        .response()
        .putHeader("content-type", "text/plain")
        .end("Hello from HTTP/3 server!");
    });

    // Start the server
    server.listen(ar -> {
      if (ar.succeeded()) {
        System.out.println(
          "HTTP/3 server started on port " + ar.result().actualPort()
        );
      } else {
        System.out.println("Failed to start server: " + ar.cause());
      }
    });
  }

  public static void main(String[] args) {
    Vertx vertx = Vertx.vertx();
    vertx.deployVerticle(new Http3VertxServer());
  }
}
