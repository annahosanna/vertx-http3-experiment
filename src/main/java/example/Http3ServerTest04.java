package example;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.*;
import io.netty.incubator.codec.http3.*;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

public class Http3FortuneServer extends AbstractVerticle {

  public static void main(String[] args) {
    Vertx.vertx().deployVerticle(new Http3FortuneServer());
  }

  @Override
  public void start() {
    QuicServer.newServer()
      .handler(channel -> {
        channel
          .pipeline()
          .addLast(new HttpServerCodec())
          .addLast(new Http2ServerUpgradeCodec())
          .addLast(new FortuneHandler());
      })
      .bind(8443)
      .addListener(
        (ChannelFutureListener) future -> {
          if (future.isSuccess()) {
            System.out.println("Server started on port 8443");
          } else {
            System.err.println("Failed to start server: " + future.cause());
          }
        }
      );
  }

  private class FortuneHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
      if (msg instanceof HttpRequest) {
        HttpRequest request = (HttpRequest) msg;

        // Check for HTTP/2 upgrade
        if (
          request
            .headers()
            .contains(HttpHeaderNames.UPGRADE, HttpHeaderValues.H2C, true)
        ) {
          ctx.writeAndFlush(
            new DefaultHttpResponse(
              HttpVersion.HTTP_1_1,
              HttpResponseStatus.SWITCHING_PROTOCOLS
            )
          );
          return;
        }

        // Send Alt-Svc header for HTTP/3
        HttpResponse response = new DefaultFullHttpResponse(
          HttpVersion.HTTP_1_1,
          HttpResponseStatus.OK
        );
        response
          .headers()
          .set(HttpHeaderNames.ALT_SVC, "h3=\":8443\"; ma=3600");

        // Generate fortune response
        JsonObject fortune = new JsonObject()
          .put("fortune", "The fortune you seek is in another protocol.");

        // Set response headers
        response
          .headers()
          .set(HttpHeaderNames.CONTENT_TYPE, "application/json")
          .set(HttpHeaderNames.CONTENT_LENGTH, fortune.toString().length());

        // Write response
        ctx
          .writeAndFlush(fortune.toString())
          .addListener(ChannelFutureListener.CLOSE);
      }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
      cause.printStackTrace();
      ctx.close();
    }
  }
}
