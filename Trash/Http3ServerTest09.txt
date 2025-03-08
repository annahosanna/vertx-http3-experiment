package example;

class VertexRouterServerExample extends SimpleChannelInboundHandler<Object> {

  private final VertexRouter router;

  public VertexRouterServerExample(VertexRouter router) {
    this.router = router;
  }

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, Object msg) {
    if (msg instanceof HttpRequest) {
      HttpRequest request = (HttpRequest) msg;

      // Create routing context
      RoutingContext routingContext = new RoutingContext(request, ctx);

      // Handle the request through vertex router
      router.accept(routingContext);
    }
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    cause.printStackTrace();
    ctx.close();
  }
}

public class Server {

  private final int port;
  private final VertexRouter router;

  public Server(int port) {
    this.port = port;
    this.router = VertexRouter.router();

    // Configure routes
    router.get("/api/:param").handler(this::handleGet);
    router.post("/api/data").handler(this::handlePost);
  }

  public void start() throws Exception {
    EventLoopGroup bossGroup = new NioEventLoopGroup();
    EventLoopGroup workerGroup = new NioEventLoopGroup();

    try {
      ServerBootstrap b = new ServerBootstrap();
      b
        .group(bossGroup, workerGroup)
        .channel(NioServerSocketChannel.class)
        .childHandler(
          new ChannelInitializer<SocketChannel>() {
            @Override
            public void initChannel(SocketChannel ch) {
              ch
                .pipeline()
                .addLast(
                  new HttpServerCodec(),
                  new HttpObjectAggregator(65536),
                  new VertexRouterServerExample(router)
                );
            }
          }
        );

      Channel ch = b.bind(port).sync().channel();
      ch.closeFuture().sync();
    } finally {
      bossGroup.shutdownGracefully();
      workerGroup.shutdownGracefully();
    }
  }

  private void handleGet(RoutingContext context) {
    String param = context.pathParam("param");
    context
      .response()
      .putHeader("content-type", "application/json")
      .end(new JsonObject().put("param", param).encode());
  }

  private void handlePost(RoutingContext context) {
    JsonObject body = context.getBodyAsJson();
    context
      .response()
      .putHeader("content-type", "application/json")
      .end(body.encode());
  }
}
