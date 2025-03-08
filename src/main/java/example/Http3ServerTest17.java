package example;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.incubator.codec.http3.Http3;
import io.netty.incubator.codec.http3.Http3HeadersFrame;
import io.netty.incubator.codec.http3.Http3ServerCodecBuilder;
import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;

public class Http3ServerTest17 {

  public static void main(String[] args) throws Exception {
    // Initialize Vert.x and create router
    Vertx vertx = Vertx.vertx();
    Router router = Router.router(vertx);

    // Add route handler for /fortunes endpoint
    router
      .get("/fortunes")
      .handler(ctx -> {
        ctx
          .response()
          .putHeader("content-type", "application/json")
          .end("{\"fortune\": \"Today is your lucky day!\"}");
      });

    // Configure HTTP/3 server
    NioEventLoopGroup group = new NioEventLoopGroup(1);
    try {
      Bootstrap bootstrap = new Bootstrap();
      bootstrap
        .group(group)
        .channel(Http3.getServerChannel())
        .handler(
          new ChannelInitializer<Channel>() {
            @Override
            protected void initChannel(Channel ch) {
              ch
                .pipeline()
                .addLast(
                  Http3ServerCodecBuilder.forServer().build(),
                  new Http3ServerHandler(router) // Custom handler that uses Vert.x router
                );
            }
          }
        );

      Channel channel = bootstrap.bind(8443).sync().channel();
      channel.closeFuture().sync();
    } finally {
      group.shutdownGracefully();
    }
  }
}

class Http3ServerHandler
  extends SimpleChannelInboundHandler<Http3HeadersFrame> {

  private final Router router;

  public Http3ServerHandler(Router router) {
    this.router = router;
  }

  // fix this
  @Override
  protected void channelRead0(
    ChannelHandlerContext ctx,
    Http3HeadersFrame frame
  ) {
    // Convert Netty request to Vert.x request and use router
    HttpServerRequest request = new VertxHttpServerRequest(frame);
    router.handle(request);
  }
}
