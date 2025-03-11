// Required imports
import io.netty.incubator.codec.http3.Http3HeadersFrame;
import io.netty.incubator.codec.http3.DefaultHttp3Headers;
import io.netty.incubator.codec.quic.QuicChannel;
import io.netty.incubator.codec.quic.QuicServerCodecBuilder;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import reactor.core.publisher.Mono;
import reactor.netty.Connection;

// Fix missing imports

public class Http3Server {

    public static void main(String[] args) {
        QuicServer.create()
            .port(8080)
            .wiretap(true) 
            .handle((context, connection) -> {
                context.addHandlerLast(new Http3FrameHandler());
                return Mono.never();
            })
            .bind()
            .block();
    }
}

class Http3FrameHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof Http3HeadersFrame) {
            Http3HeadersFrame headersFrame = (Http3HeadersFrame) msg;
            DefaultHttp3Headers headers = (DefaultHttp3Headers) headersFrame.headers();

            // Process HTTP/3 headers
            System.out.println("Received HTTP/3 headers: " + headers);

            // Release the frame to prevent memory leaks
            headersFrame.release();
        }

        // Forward the message to the next handler in pipeline
        ctx.fireChannelRead(msg);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}