package example;

import io.netty.buffer.ByteBuf;
import io.netty.incubator.codec.http3.Http3DataFrame;
import io.netty.incubator.codec.http3.Http3HeadersFrame;
import io.netty.incubator.codec.http3.Http3RequestStreamInboundHandler;
import io.netty.incubator.codec.http3.Http3ServerBuilder;
import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

public class Http3FortuneServer {

  private final Vertx vertx;
  private final Router router;

  public Http3FortuneServer() {
    this.vertx = Vertx.vertx();
    this.router = Router.router(vertx);

    // Set up fortune route
    router.get("/fortunes").handler(this::handleFortunes);
  }

  private void handleFortunes(RoutingContext ctx) {
    ctx
      .response()
      .putHeader("content-type", "application/json")
      .end("{\"fortune\": \"Your code will compile on the first try.\"}");
  }

  public void start() throws Exception {
    Http3ServerBuilder.create()
      .host("localhost")
      .port(443)
      .handler(
        new Http3RequestStreamInboundHandler() {
          @Override
          protected void channelRead(
            ChannelHandlerContext ctx,
            Http3HeadersFrame frame
          ) {
            String path = frame.headers().path().toString();

            if (path.equals("/fortunes")) {
              // Create vertx request context
              RoutingContext routingContext = router.createContext(
                new VertxHttpServerRequest(frame)
              );

              // Handle the request through vertx router
              router.handle(routingContext);

              // Send response back through http3
              Http3HeadersFrame responseHeaders =
                new DefaultHttp3HeadersFrame();
              responseHeaders
                .headers()
                .status("200")
                .add("content-type", "application/json");

              ctx.write(responseHeaders);

              ByteBuf content = ctx.alloc().buffer();
              content.writeBytes(routingContext.response().getBody());

              ctx.write(new DefaultHttp3DataFrame(content));
              ctx.flush();
            }
          }
        }
      )
      .build();
  }

  public static void main(String[] args) throws Exception {
    new Http3FortuneServer().start();
  }
}
