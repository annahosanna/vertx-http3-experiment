import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http3.Http3Frame;
import io.netty.handler.codec.http3.Http3HeadersFrame;
import io.netty.handler.codec.http3.DefaultHttp3HeadersFrame;
import io.netty.handler.codec.http3.Http3;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;

public class Http3FortuneServer extends SimpleChannelInboundHandler<Http3Frame> {

    private final Vertx vertx;
    private final Router router;

    public Http3FortuneServer() {
        this.vertx = Vertx.vertx();
        this.router = Router.router(vertx);

        // Set up fortune endpoint
        router.get("/fortunes").handler(ctx -> {
            JsonObject fortune = new JsonObject()
                .put("message", "Your lucky numbers are: " + Math.random() * 100);

            ctx.response()
               .putHeader("content-type", "application/json")
               .end(fortune.encode());
        });
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Http3Frame frame) throws Exception {
        if (frame instanceof Http3HeadersFrame) {
            Http3HeadersFrame headersFrame = (Http3HeadersFrame) frame;
            String path = headersFrame.headers().get(":path");

            if ("/fortunes".equals(path)) {
                // Create response headers
                DefaultHttp3HeadersFrame responseHeaders = new DefaultHttp3HeadersFrame();
                responseHeaders.headers()
                    .status(HttpResponseStatus.OK.codeAsText())
                    .add(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON);

                // Generate fortune response
                JsonObject fortune = new JsonObject()
                    .put("message", "Your lucky numbers are: " + Math.random() * 100);

                // Write response
                ctx.write(responseHeaders);
                ctx.writeAndFlush(Http3.toHttp3DataFrame(fortune.toString()));
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}