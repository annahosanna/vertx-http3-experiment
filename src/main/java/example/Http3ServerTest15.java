package example;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.incubator.codec.http3.Http3;
import io.netty.incubator.codec.http3.Http3ServerBuilder;
import io.netty.incubator.codec.quic.QuicServerCodecBuilder;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.web.Router;

public class Http3Server {

  public static void main(String[] args) throws Exception {
    // Initialize Vert.x
    Vertx vertx = Vertx.vertx();

    // Create Router
    Router router = Router.router(vertx);

    // Add fortune route
    router
      .route(HttpMethod.GET, "/fortunes")
      .handler(ctx -> {
        JsonArray fortunes = new JsonArray()
          .add("Fortune favors the bold")
          .add("A journey of a thousand miles begins with a single step")
          .add("You will find happiness in unexpected places");

        ctx
          .response()
          .putHeader("content-type", "application/json")
          .end(fortunes.encode());
      });

    // Configure SSL context for QUIC/HTTP3
    SslContext sslContext = SslContext.builder()
      .keyFile(new File("server.key"))
      .certFile(new File("server.crt"))
      .build();

    // Create HTTP3 server
    Http3ServerBuilder.create()
      .sslContext(sslContext)
      .codec(
        QuicServerCodecBuilder.create()
          .handler(
            new ChannelInitializer<SocketChannel>() {
              @Override
              protected void initChannel(SocketChannel ch) {
                ch.pipeline().addLast(new Http3ServerHandler(router));
              }
            }
          )
      )
      .bindAddress("localhost")
      .port(443)
      .build()
      .start();

    System.out.println("HTTP/3 Server started on port 443");
  }
}

@ChannelHandler.Sharable
class Http3ServerHandler
  extends SimpleChannelInboundHandler<Http3RequestStreamInbound> {

  private final Router router;

  public Http3ServerHandler(Router router) {
    this.router = router;
  }

  @Override
  protected void channelRead0(
    ChannelHandlerContext ctx,
    Http3RequestStreamInbound msg
  ) {
    msg
      .stream()
      .addHandler(
        new Http3HeadersFrameHandler() {
          @Override
          public void onHeadersRead(
            ChannelHandlerContext ctx,
            Http3HeadersFrame frame
          ) {
            String path = frame.headers().get(":path");

            if ("/fortunes".equals(path)) {
              router.handle(new Http3RoutingContext(ctx, msg, frame));
            } else {
              Http3Headers headers = Http3Headers.newHeaders()
                .status("404")
                .setInt("content-length", 0);

              ctx.writeAndFlush(new DefaultHttp3HeadersFrame(headers));
            }
          }
        }
      );
  }
}
