import io.netty.incubator.codec.http3.Http3ServerCodec;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;

// no such method as addCodec
public class Http3Server extends AbstractVerticle {

  @Override
  public void start(Promise<Void> startPromise) {
    // Create router
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

    // Configure HTTP/3 server
    HttpServer server = vertx
      .createHttpServer()
      .addCodec(new Http3ServerCodec())
      .requestHandler(router);

    // Start server
    server
      .listen(8443, "0.0.0.0")
      .onSuccess(http -> {
        System.out.println(
          "HTTP/3 server started on port " + http.actualPort()
        );
        startPromise.complete();
      })
      .onFailure(startPromise::fail);
  }
}
