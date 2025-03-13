package example;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.incubator.codec.http3.*;
import io.netty.incubator.codec.http3.Http3DataFrame;
import io.netty.incubator.codec.http3.Http3HeadersFrame;
// import java.sql.Connection;
// import java.sql.PreparedStatement;
// import java.sql.ResultSet;
import io.netty.util.ReferenceCountUtil;

public class FortuneHeaderFrameHandler
  extends SimpleChannelInboundHandler<Http3DataFrame> {

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, Http3DataFrame frame)
    throws Exception {
    // String fortune = getRandomFortune();
    String fortune = "Yay it worked";

    Http3HeadersFrame headers = new DefaultHttp3HeadersFrame();
    headers.headers().status("200").add("content-type", "text/plain");

    ctx.write(headers);

    ByteBuf content = ctx.alloc().buffer();
    content.writeBytes(fortune.getBytes());
    DefaultHttp3DataFrame df = new DefaultHttp3DataFrame(content);
    ctx.writeAndFlush(df);
    df.release();
    // content.release();
    ReferenceCountUtil.release(content);
  }
  //  private String getRandomFortune() throws Exception {
  //    String fortune = "";
  //    try (Connection conn = DatabaseConnection.getConnection()) {
  //      PreparedStatement stmt = conn.prepareStatement(
  //        "SELECT text FROM fortunes ORDER BY RANDOM() LIMIT 1"
  //      );
  //      ResultSet rs = stmt.executeQuery();
  //      if (rs.next()) {
  //        fortune = rs.getString("text");
  //      }
  //    }
  //    return fortune;
  //  }
}
