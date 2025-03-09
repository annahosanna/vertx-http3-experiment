import io.netty.bootstrap.Bootstrap;
import io.netty.incubator.codec.http3.*;
import io.netty.incubator.codec.quic.*;
import io.netty.channel.*;
import io.netty.buffer.Unpooled;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Random;

public class Http3FortuneServer {
    private static final List<String> FORTUNES = List.of(
        "You will find happiness",
        "Good fortune awaits",
        "Be patient and good things will come",
        "Adventure lies ahead"
    );
    private static final Random RANDOM = new Random();
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static void main(String[] args) throws Exception {
        QuicSslContext context = QuicSslContextBuilder.forServer(
                                    "cert.pem", "key.pem").build();

        EventLoopGroup group = new DefaultEventLoopGroup();
        try {
            Bootstrap bs = new Bootstrap();
            bs.group(group)
              .channel(QuicChannel.class)
              .handler(new ChannelInitializer<Channel>() {
                  @Override
                  protected void initChannel(Channel ch) {
                      ChannelPipeline p = ch.pipeline();
                      p.addLast(new Http3ServerConnectionHandler());
                      p.addLast(new Http3ServerHandler());
                  }
              });

            Channel channel = bs.bind(8443).sync().channel();
            channel.closeFuture().sync();
        } finally {
            group.shutdownGracefully();
        }
    }

    private static class Http3ServerHandler extends Http3RequestStreamInboundHandler {
        @Override
        protected void channelRead(ChannelHandlerContext ctx, Http3HeadersFrame headersFrame,
                                 boolean isLast) throws Exception {
            String path = headersFrame.headers().path().toString();

            if ("/fortune".equals(path)) {
                String fortune = FORTUNES.get(RANDOM.nextInt(FORTUNES.size()));
                String jsonResponse = MAPPER.writeValueAsString(
                    new Fortune(fortune)
                );

                Http3Headers headers = new DefaultHttp3Headers();
                headers.status("200");
                headers.add("content-type", "application/json");

                ctx.write(new DefaultHttp3HeadersFrame(headers));
                ctx.write(new DefaultHttp3DataFrame(
                    Unpooled.wrappedBuffer(jsonResponse.getBytes())
                ));
                ctx.flush();
            } else {
                Http3Headers headers = new DefaultHttp3Headers();
                headers.status("404");
                ctx.write(new DefaultHttp3HeadersFrame(headers));
                ctx.flush();
            }
        }
    }

    private static class Fortune {
        private String message;

        public Fortune(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }
}