package example;

// A java example of using a project reactor, create a QuicServer object and then use the bind method of the object to attach a channelhandler to the context. Use the handler to read HTTP3HeaderFrames. Place the handler in a seperate class. Use the netty http3 incubator. Document the imports.

// Some of the imports are wrong and need to be fixed

// Required imports
import io.netty.incubator.codec.http3.Http3HeadersFrame;
import io.netty.incubator.codec.http3.Http3FrameCodecBuilder; 
import io.netty.incubator.codec.quic.QuicChannel;
import io.netty.incubator.codec.quic.QuicServer;
import io.netty.incubator.codec.quic.QuicServerBuilder;
import reactor.core.publisher.Mono;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class Http3ServerExample {
    public static void main(String[] args) {
        QuicServer server = QuicServerBuilder.create()
            .handler(new Http3ServerHandler())
            .build();

        Mono<Void> bind = server.bind(8443)
            .doOnSuccess(v -> System.out.println("Server started on port 8443"))
            .doOnError(e -> System.err.println("Failed to start server: " + e.getMessage()));

        bind.block();
    }
}

@ChannelHandler.Sharable
class Http3ServerHandler extends SimpleChannelInboundHandler<Http3HeadersFrame> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Http3HeadersFrame frame) {
        System.out.println("Received HTTP/3 headers: " + frame.headers().toString());

        // Process the headers
        frame.headers().forEach(entry -> {
            String name = entry.getKey();
            String value = entry.getValue();
            System.out.println(name + ": " + value);
        });
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}