import io.netty.channel.ChannelHandler;
import io.netty.incubator.codec.http3.*;
import io.netty.incubator.codec.quic.*;
import java.util.concurrent.CompletableFuture;

public class Http3Server {
    public static void main(String[] args) throws Exception {
        QuicServer server = QuicServer.builder()
            .certificate(SelfSignedCertificate.create())
            .handler(new QuicServerCodecBuilder()
                .streamHandler(new Http3Handler())
                .build())
            .bind(8443)
            .sync()
            .channel(); 
    }
}

class Http3Handler implements ChannelHandler {
    private final RequestHandler requestHandler;
    private final PromiseManager promiseManager;

    public Http3Handler() {
        this.requestHandler = new RequestHandler();
        this.promiseManager = new PromiseManager();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof Http3HeadersFrame) {
            Http3HeadersFrame headersFrame = (Http3HeadersFrame) msg;
            requestHandler.handleRequest(ctx, headersFrame)
                .thenAccept(response -> {
                    promiseManager.sendResponse(ctx, response);
                });
        }
    }
}

class RequestHandler {
    public CompletableFuture<Http3HeadersFrame> handleRequest(
        ChannelHandlerContext ctx, 
        Http3HeadersFrame request) {

        return CompletableFuture.supplyAsync(() -> {
            return new DefaultHttp3HeadersFrame()
                .headers()
                .status("200")
                .add("content-type", "text/plain");
        });
    }
}

class PromiseManager {
    public void sendResponse(ChannelHandlerContext ctx, Http3HeadersFrame response) {
        QuicStreamChannel stream = (QuicStreamChannel) ctx.channel();
        stream.writeAndFlush(response);
        stream.close();
    }
}