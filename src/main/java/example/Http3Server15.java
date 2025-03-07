import io.netty.handler.codec.http3.HttpCodecType;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.net.QuicServerOptions;
import io.vertx.ext.web.Router;

public class Http3Server extends AbstractVerticle {

  @Override
  public void start() throws Exception {
    Router router = Router.router(vertx);

    router.route("/").handler(ctx -> {
      ctx.response()
         .putHeader("content-type", "text/plain")
         .end("Hello from HTTP/3 server!");
    });

    QuicServerOptions quicOptions = new QuicServerOptions()
      .setPort(8443)
      .setSslEngineOptions(new QuicSslEngineOptions()
        .setKeyStoreOptions(new JksOptions()
          .setPath("keystore.jks")
          .setPassword("password"))
      );

    HttpServerOptions serverOptions = new HttpServerOptions()
      .setUseAlpn(true)
      .setQuic(quicOptions);

    vertx.createHttpServer(serverOptions)
      .requestHandler(router)
      .listen(res -> {
        if (res.succeeded()) {
          System.out.println("Server started on port 8443");
        } else {
          System.out.println("Failed to start server: " + res.cause());
        }
      });
  }

  public static void main(String[] args) {
    Vertx vertx = Vertx.vertx();
    vertx.deployVerticle(new Http3Server());
  }
}