import io.netty.incubator.codec.http3.DefaultHttp3Headers;
import io.netty.incubator.codec.http3.Http3;
import io.netty.incubator.codec.http3.Http3ServerCodecBuilder;
import io.netty.incubator.codec.quic.QuicServerCodecBuilder;
import io.netty.channel.*;
import io.netty.buffer.ByteBuf;

public class Http3Server {
    public static void main(String[] args) throws Exception {
        QuicServerCodecBuilder.create()
            .certificateChain("cert.pem")
            .privateKey("key.pem") 
            .applicationProtocols(Http3.supportedApplicationProtocols())
            .maxIdleTimeout(5000, TimeUnit.MILLISECONDS)
            .initialMaxData(10000000)
            .initialMaxStreamDataBidirectionalLocal(1000000)
            .build()
            .bind(8443)
            .sync()
            .channel();

        Channel server = Http3ServerCodecBuilder.create()
            .encoderEnforceMaxConcurrentStreams(true)
            .initialMaxData(1048576)
            .maxHeaderListSize(8192)
            .serverCodec(new Http3ServerCodec())
            .handler(new ChannelInitializer<Channel>() {
                @Override
                protected void initChannel(Channel ch) {
                    ch.pipeline().addLast(new SimpleChannelInboundHandler<ByteBuf>() {
                        @Override
                        protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) {
                            DefaultHttp3Headers headers = new DefaultHttp3Headers();
                            headers.status("200");
                            headers.add("content-type", "text/plain");

                            ByteBuf content = ctx.alloc().buffer();
                            content.writeBytes("Hello HTTP/3".getBytes());

                            ctx.write(headers);
                            ctx.write(content);
                            ctx.flush();
                        }
                    });
                }
            })
            .build();

        server.closeFuture().sync();
    }
}