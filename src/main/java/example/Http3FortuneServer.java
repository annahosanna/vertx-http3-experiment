package exampple;

// This is pretty good but doesn't use a vertx route
// and it doesn't fully inialize the server (tls key etc.)

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
