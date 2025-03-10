import io.netty.channel.ChannelHandler;
import io.netty.incubator.codec.http3.Http3RequestStreamInboundHandler;
import io.netty.incubator.codec.quic.QuicChannel;
import io.netty.incubator.codec.quic.QuicServerCodecBuilder;
import io.netty.incubator.codec.quic.QuicStreamType;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class FortuneServer {

  private static final int PORT = 8443;
  private final List<String> fortunes = new ArrayList<>();

  public void start() throws Exception {
    QuicServerCodecBuilder.create()
      .certificateChain("cert.pem")
      .privateKey("key.pem")
      .handler(new FortuneChannelInitializer(new FortuneHandler(fortunes)))
      .bind(new InetSocketAddress(PORT))
      .sync();
  }
}

class FortuneHandler extends Http3RequestStreamInboundHandler {

  private final List<String> fortunes;

  public FortuneHandler(List<String> fortunes) {
    this.fortunes = fortunes;
  }

  @Override
  protected void channelRead(
    ChannelHandlerContext ctx,
    Http3HeadersFrame headersFrame,
    boolean isLast
  ) {
    String path = headersFrame.headers().path().toString();

    if (path.equals("/fortune")) {
      if (headersFrame.method().name().equals("GET")) {
        handleGetFortune(ctx);
      } else if (headersFrame.method().name().equals("POST")) {
        handleAddFortune(ctx);
      }
    }
  }

  private void handleGetFortune(ChannelHandlerContext ctx) {
    FortuneOperations.getFortune(fortunes).thenAccept(fortune -> {
      Http3HeadersFrame responseHeaders = new DefaultHttp3HeadersFrame();
      responseHeaders.headers().status("200").add("content-type", "text/plain");

      ctx.write(responseHeaders);
      ctx.writeAndFlush(
        new DefaultHttp3DataFrame(
          Unpooled.copiedBuffer(fortune, CharsetUtil.UTF_8)
        )
      );
    });
  }

  private void handleAddFortune(ChannelHandlerContext ctx) {
    FortuneOperations.addFortune(ctx, fortunes).thenAccept(success -> {
      Http3HeadersFrame responseHeaders = new DefaultHttp3HeadersFrame();
      responseHeaders.headers().status(success ? "201" : "400");
      ctx.writeAndFlush(responseHeaders);
    });
  }
}

class FortuneOperations {

  public static CompletableFuture<String> getFortune(List<String> fortunes) {
    return CompletableFuture.supplyAsync(() -> {
      if (fortunes.isEmpty()) {
        return "No fortunes available";
      }
      int index = (int) (Math.random() * fortunes.size());
      return fortunes.get(index);
    });
  }

  public static CompletableFuture<Boolean> addFortune(
    ChannelHandlerContext ctx,
    List<String> fortunes
  ) {
    return CompletableFuture.supplyAsync(() -> {
      try {
        if (ctx.channel().attr(CONTENT_KEY).get() != null) {
          String fortune = ctx.channel().attr(CONTENT_KEY).get().toString();
          fortunes.add(fortune);
          return true;
        }
        return false;
      } catch (Exception e) {
        return false;
      }
    });
  }
}

class FortuneChannelInitializer extends ChannelInitializer<QuicChannel> {

  private final ChannelHandler handler;

  public FortuneChannelInitializer(ChannelHandler handler) {
    this.handler = handler;
  }

  @Override
  protected void initChannel(QuicChannel ch) {
    ch.pipeline().addLast(handler);
  }
}
