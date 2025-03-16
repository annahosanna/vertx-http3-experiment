package example;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.incubator.codec.http3.*;
import io.netty.incubator.codec.quic.*;

public class Http3ServerExample01 {

  public static void main(String[] args) throws Exception {
    // Configure the server
	  
	  // Create a new ChannelHandler
	  QuicServerCodecBuilder serverBuilder = Http3.newQuicServerCodecBuilder().
	  // Http3ServerBuilder serverBuilder = Http3ServerBuilder.create()
      .host("localhost")
      .port(8443)
      .certificateChain("cert.pem")
      .privateKey("key.pem");

    // Add handler for HTTP/3 requests
    serverBuilder.handler(
      new SimpleChannelInboundHandler<Http3HeadersFrame>() {
        @Override
        protected void channelRead0(
          ChannelHandlerContext ctx,
          Http3HeadersFrame frame
        ) {
          // Create response headers
          Http3Headers headers = Http3Headers.newHeaders()
            .status("200")
            .add("content-type", "text/plain");

          // Create response content
          String content = "Hello HTTP/3!";
          Http3DataFrame data = new DefaultHttp3DataFrame(
            ctx.alloc().buffer().writeBytes(content.getBytes())
          );

          // Write response
          ctx.write(new DefaultHttp3HeadersFrame(headers));
          ctx.write(data);
          ctx.flush();
        }
      }
    );

    // Start the server
    Http3Server server = serverBuilder.build();
    server.start().sync();
    server.closeFuture().sync();
  }
}
