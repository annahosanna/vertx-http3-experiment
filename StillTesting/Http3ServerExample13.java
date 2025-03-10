import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import reactor.core.publisher.Mono;
import reactor.netty.http.HttpProtocol;

public class Http3Server {
    private final int port;

    public Http3Server(int port) {
        this.port = port;
    }

    public void start() throws Exception {
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .handler(new LoggingHandler(LogLevel.INFO))
                .childHandler(new ServerInitializer());

            Channel ch = b.bind(port).sync().channel();
            ch.closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}

class ServerInitializer extends ChannelInitializer<SocketChannel> {
    @Override
    protected void initChannel(SocketChannel ch) {
        ChannelPipeline p = ch.pipeline();
        p.addLast(new HttpServerCodec());
        p.addLast(new RequestHandler());
    }
}

class RequestHandler extends SimpleChannelInboundHandler<Object> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) {
        handleRequest(msg)
            .subscribe(response -> {
                ctx.writeAndFlush(response);
            });
    }

    private Mono<String> handleRequest(Object request) {
        return Mono.just("Response from server");
    }
}

class FutureHandler {
    public static CompletableFuture<String> processAsync(String data) {
        return CompletableFuture.supplyAsync(() -> {
            // Simulated async processing
            try {
                Thread.sleep(100);
                return "Processed: " + data;
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
    }
}

public class Main {
    public static void main(String[] args) throws Exception {
        int port = 8443;
        Http3Server server = new Http3Server(port);
        server.start();
    }
}