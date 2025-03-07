import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;

public class NettyVertxServer {

    private final int port;

    public NettyVertxServer(int port) {
        this.port = port;
    }

    public void start() throws Exception {
        // Create Vertx instance and router
        Vertx vertx = Vertx.vertx();
        Router router = Router.router(vertx);

        // Add routes
        router.get("/api/hello").handler(ctx -> {
            ctx.response()
               .putHeader("content-type", "text/plain")
               .end("Hello from Netty + Vertx!");
        });

        // Configure Netty
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
             .channel(NioServerSocketChannel.class)
             .childHandler(new ChannelInitializer<SocketChannel>() {
                 @Override
                 protected void initChannel(SocketChannel ch) {
                     ch.pipeline().addLast(new HttpServerHandler(router));
                 }
             });

            // Bind and start to accept incoming connections
            ChannelFuture f = b.bind(port).sync();
            System.out.println("Server started on port " + port);

            // Wait until the server socket is closed
            f.channel().closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
            vertx.close();
        }
    }

    public static void main(String[] args) throws Exception {
        new NettyVertxServer(8080).start();
    }
}