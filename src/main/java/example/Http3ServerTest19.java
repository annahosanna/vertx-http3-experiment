package example;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.incubator.codec.http3.Http3ServerBuilder;
import io.netty.incubator.codec.http3.Http3ServerConnectionHandler;
import io.netty.incubator.codec.http3.Http3ServerStreamFrameListener;
import io.vertx.core.Vertx;
import io.vertx.core.http.impl.HttpServerRequestImpl;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

public class Http3Server {

    private static final int PORT = 8443;
    private final Vertx vertx;
    private final Router router;

    public Http3Server() {
        this.vertx = Vertx.vertx();
        this.router = Router.router(vertx);
        setupRoutes();
    }

    private void setupRoutes() {
        router.get("/").handler(this::handleRequest);
    }

    private void handleRequest(RoutingContext routingContext) {
        routingContext.response()
            .putHeader("content-type", "text/plain")
            .end("Hello from HTTP/3 server!");
    }

    public void start() throws Exception {
        Http3ServerBuilder.newBuilder()
            .sslContext(SslContextBuilder.forServer(...).build())
            .host("localhost")
            .port(PORT)
            .handler(new Http3ServerConnectionHandler() {
                @Override
                protected Http3ServerStreamFrameListener newStreamFrameListener() {
                    return new Http3ServerStreamFrameListener() {
                        @Override
                        public void onHeadersRead(ChannelHandlerContext ctx, long streamId,
                            Http3Headers headers, boolean endStream) {

                            // Convert Netty request to Vert.x request
                            HttpServerRequestImpl vertxRequest = new HttpServerRequestImpl(
                                vertx.createHttpServer(),
                                headers.method().toString(),
                                headers.path().toString(),
                                headers.scheme().toString(),
                                headers.authority().toString(),
                                null, // socketAddress
                                null, // remoteAddress
                                true, // ssl
                                headers,
                                null  // netSocket
                            );

                            // Handle with router
                            router.handle(vertxRequest);

                            // Send response back through Netty
                            Http3Headers responseHeaders = new DefaultHttp3Headers()
                                .status("200")
                                .set("content-type", "text/plain");

                            ctx.write(responseHeaders);
                            ByteBuf content = ctx.alloc().buffer();
                            content.writeBytes("Hello from HTTP/3 server!".getBytes());
                            ctx.writeAndFlush(content);
                        }
                    };
                }
            })
            .build()
            .bind()
            .sync();

        System.out.println("HTTP/3 Server started on port " + PORT);
    }

    public static void main(String[] args) throws Exception {
        new Http3Server().start();
    }
}
