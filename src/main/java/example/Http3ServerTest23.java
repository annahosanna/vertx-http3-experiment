package example;

import io.netty.incubator.codec.http3.Http3DataFrame;
import io.netty.incubator.codec.http3.Http3HeadersFrame;
import io.netty.incubator.codec.http3.Http3ServerBuilder;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;

public class Http3Server {

  public static void main(String[] args) throws Exception {
    Vertx vertx = Vertx.vertx();
    Router router = Router.router(vertx);

    // Create route handler
    router
      .route("/")
      .handler(routingContext -> {
        routingContext
          .response()
          .putHeader("content-type", "text/plain")
          .end("Hello from HTTP/3!");
      });

    // Create HTTP/3 server
    Http3ServerBuilder.newBuilder()
      .port(443)
      .certFile("cert.pem")
      .keyFile("key.pem")
      .handler((ctx, headers, body) -> {
        // Convert Netty request to Vertx request
        HttpServerRequest vertxRequest = new VertxHttpServerRequestAdapter(
          ctx,
          headers
        );

        // Process with router
        router.handle(vertxRequest);

        // Send response
        ctx.writeAndFlush(
          new Http3HeadersFrame()
            .status("200")
            .add("content-type", "text/plain")
            .add("alt-svc", "h3=\":443\"; ma=2592000")
        ); // Add Alt-Svc header

        ctx.writeAndFlush(
          new Http3DataFrame()
            .content(
              Unpooled.copiedBuffer(
                "Hello from HTTP/3!",
                StandardCharsets.UTF_8
              )
            )
        );

        return ctx.newSucceededFuture();
      })
      .build()
      .start();
  }

  // Adapter class to convert Netty request to Vertx request
  static class VertxHttpServerRequestAdapter implements HttpServerRequest {

    private final Http3StreamChannel ctx;
    private final Http3HeadersFrame headers;

    public VertxHttpServerRequestAdapter(
      Http3StreamChannel ctx,
      Http3HeadersFrame headers
    ) {
      this.ctx = ctx;
      this.headers = headers;
    }

    // Implement HttpServerRequest methods...
    @Override
    public String uri() {
      return headers.path().toString();
    }

    @Override
    public String method() {
      return headers.method().toString();
    }
    // Other required implementations...
  }
}
