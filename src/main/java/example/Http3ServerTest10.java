import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http3.*;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.Router;

public class HttpServer extends SimpleChannelInboundHandler<Http3HeadersFrame> {

    private final Router router;
    private final Vertx vertx;

    public HttpServer(Vertx vertx, Router router) {
        this.vertx = vertx;
        this.router = router;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Http3HeadersFrame msg) {
        // Create VertX request object from Netty Http3 headers
        HttpServerRequest request = createVertxRequest(msg);

        // Handle request through Vertex router
        router.handle(request);

        // Create HTTP/3 response
        Http3Headers headers = new DefaultHttp3Headers();
        headers.status("200");
        headers.add("content-type", "text/plain");

        // Write response headers
        ctx.writeAndFlush(new DefaultHttp3HeadersFrame(headers));

        // Write response data
        ctx.writeAndFlush(new DefaultHttp3DataFrame(request.response().getBody()));
    }

    private HttpServerRequest createVertxRequest(Http3HeadersFrame frame) {
        // Convert Netty HTTP/3 headers to VertX request
        HttpServerRequest request = vertx.createHttpServerRequest();
        Http3Headers headers = frame.headers();

        headers.forEach(header -> {
            request.headers().add(header.getKey(), header.getValue());
        });

        return request;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}