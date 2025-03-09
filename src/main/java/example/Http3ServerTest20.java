package example;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.incubator.codec.http3.Http3ServerConnectionHandler;
// import io.netty.incubator.codec.http3.Http3ServerHandler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.Router;

public class Http3ServerTest20 {

  private final Vertx vertx;
  private final Router router;

  public Http3ServerTest20() {
    this.vertx = Vertx.vertx();
    this.router = Router.router(vertx);

    // Setup route
    router
      .route("/hello")
      .handler(ctx -> {
        ctx
          .response()
          .putHeader("content-type", "text/plain")
          .end("Hello from HTTP/3 server!");
      });
  }

  public void start() {
    // Configure HTTP/3 server
	  // Http3ServerConnection handler is the wrong type
    Http3ServerConnectionHandler http3Handler =
      new Http3ServerConnectionHandler(
        new ChannelHandler() {
);
          }
        }
      );

    // Start server on port 443
    ServerBootstrap bootstrap = new ServerBootstrap()
      .channel(NioServerSocketChannel.class)
      .childHandler(http3Handler)
      .bind(443);
  }

  public static void main(String[] args) {
    new Http3ServerTest20().start();
  }
}

