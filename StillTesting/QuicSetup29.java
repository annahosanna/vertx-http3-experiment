//Imports
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.incubator.codec.quic.QuicChannel;
import io.netty.incubator.codec.quic.QuicServerCodecBuilder;
import io.netty.incubator.codec.quic.QuicServer;
import reactor.core.publisher.Mono;
import reactor.netty.Connection;

public class QuicServerExample {
    public static void main(String[] args) {
        // Create QUIC server
        QuicServer quicServer = QuicServer.create()
            .port(8080)
            .secure(spec -> spec
                .key("path/to/private.key")
                .cert("path/to/cert.crt"))
            .wiretap(true);

        // Bind handler to server context
        Connection connection = quicServer
            .handle((in, out) -> {
                in.withConnection(conn -> {
                    QuicChannel channel = (QuicChannel) conn.channel();
                    channel.pipeline().addLast(new CustomQuicHandler());
                });
                return Mono.never();
            })
            .bindNow();

        connection.onDispose().block();
    }
}

class CustomQuicHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        // Handle incoming messages
        ctx.fireChannelRead(msg);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        // Connection established
        System.out.println("New QUIC connection established");
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}