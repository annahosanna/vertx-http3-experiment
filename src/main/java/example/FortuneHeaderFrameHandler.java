package example;

import io.netty.handler.ssl.ApplicationProtocolConfig;
import io.netty.handler.ssl.SslContextBuilder;
// Required Imports
import io.netty.incubator.codec.http3.Http3HeadersFrame;
import io.netty.incubator.codec.http3.Http3ServerConnectionHandler;
import reactor.core.publisher.Mono;
import reactor.netty.quic.QuicServer;

// Custom handler classes
class FortuneHeaderFrameHandler extends Http3ServerConnectionHandler {

  private static final String[] FORTUNES = {
    "Today is your lucky day!",
    "Good things come to those who wait",
    "A pleasant surprise is coming your way",
  };

  @Override
  protected void channelRead0(
    ChannelHandlerContext ctx,
    Http3HeadersFrame frame
  ) {
    String fortune = FORTUNES[new Random().nextInt(FORTUNES.length)];
    Http3HeadersFrame response = new DefaultHttp3HeadersFrame()
      .status(HttpResponseStatus.OK.codeAsText())
      .add("content-type", "text/plain")
      .add("content-length", fortune.length());

    ctx.writeAndFlush(response);
    ctx.writeAndFlush(
      new DefaultHttp3DataFrame(Unpooled.wrappedBuffer(fortune.getBytes()))
    );
  }
}
