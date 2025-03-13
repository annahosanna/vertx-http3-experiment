package example;

import io.netty.buffer.Unpooled;
// import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.incubator.codec.http3.DefaultHttp3DataFrame;
import io.netty.incubator.codec.http3.DefaultHttp3Headers;
import io.netty.incubator.codec.http3.DefaultHttp3HeadersFrame;
// import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.incubator.codec.http3.Http3DataFrame;
import io.netty.incubator.codec.http3.Http3Headers;
import io.netty.incubator.codec.http3.Http3HeadersFrame;
import io.netty.incubator.codec.http3.Http3RequestStreamInboundHandler;
import java.util.Random;

public class Http3FortuneStreamHandler
  extends Http3RequestStreamInboundHandler {

  private static final String[] FORTUNES = {
    "Life is what happens while you're busy making other plans",
    "Today is the first day of the rest of your life",
    "The only constant in life is change",
  };

  @Override
  protected void channelRead(
    ChannelHandlerContext ctx,
    Http3HeadersFrame headersFrame
  ) throws Exception {
    String path = headersFrame.headers().path().toString();

    if ("/fortune".equals(path)) {
      Random RANDOM = new Random();
      String fortune = FORTUNES[RANDOM.nextInt(FORTUNES.length)];
      //String jsonResponse = MAPPER.writeValueAsString(
      String startOfString = new String("{fortune:\"");
      String endOfString = new String("\"}");
      String jsonResponse = startOfString.concat(fortune).concat(endOfString);

      Http3Headers headers = new DefaultHttp3Headers();
      headers.status("200");
      headers.add("content-type", "application/json");

      ctx.write(new DefaultHttp3HeadersFrame(headers));
      ctx.write(
        new DefaultHttp3DataFrame(
          Unpooled.wrappedBuffer(jsonResponse.getBytes())
        )
      );
      ctx.flush();
    } else {
      Http3Headers headers = new DefaultHttp3Headers();
      headers.status("404");
      ctx.write(new DefaultHttp3HeadersFrame(headers));
      ctx.flush();
    }
  }

  @Override
  protected void channelRead(ChannelHandlerContext ctx, Http3DataFrame frame) {
    System.out.println("Received data frame");
  }

  @Override
  public void channelReadComplete(ChannelHandlerContext ctx) {
    ctx.flush();
  }

  @Override
  protected void channelInputClosed(ChannelHandlerContext ctx)
    throws Exception {
    // TODO Auto-generated method stub
    ctx.close();
  }
}
