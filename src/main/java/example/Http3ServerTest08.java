import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http3.*;

public class Http3Server {
    private int port;

    public Http3Server(int port) {
        this.port = port;
    }

    public void run() throws Exception {
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
             .channel(NioServerSocketChannel.class)
             .childHandler(new ChannelInitializer<SocketChannel>() {
                 @Override
                 public void initChannel(SocketChannel ch) throws Exception {
                     ch.pipeline().addLast(
                         new Http3FrameCodec(),
                         new Http3ConnectionHandler(),
                         new Http3ServerHandler());
                 }
             })
             .option(ChannelOption.SO_BACKLOG, 128)
             .childOption(ChannelOption.SO_KEEPALIVE, true);

            // Bind and start to accept incoming connections.
            b.bind(port).sync().channel().closeFuture().sync();
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }

    public static void main(String[] args) throws Exception {
        int port = 8080;
        if (args.length > 0) {
            port = Integer.parseInt(args[0]);
        }

        new Http3Server(port).run();
    }
}

class Http3ServerHandler extends Http3FrameHandler {
    @Override
    public void channelRead0(ChannelHandlerContext ctx, Http3Frame frame) {
        if (frame instanceof Http3HeadersFrame) {
            Http3HeadersFrame headersFrame = (Http3HeadersFrame) frame;

            // Create response headers
            Http3Headers headers = new DefaultHttp3Headers();
            headers.status("200");
            headers.add("content-type", "text/plain");

            // Send headers
            ctx.write(new DefaultHttp3HeadersFrame(headers));

            // Send data
            ByteBuf data = ctx.alloc().buffer();
            data.writeBytes("Hello HTTP/3 World!".getBytes());
            ctx.write(new DefaultHttp3DataFrame(data));

            ctx.flush();
        }
    }
}