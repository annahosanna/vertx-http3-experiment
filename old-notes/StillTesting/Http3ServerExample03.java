// Example 1: Basic HTTP/3 Server
import io.netty.incubator.codec.http3.*;
import io.netty.incubator.codec.quic.*;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;

public class SimpleHttp3Server {
    public static void main(String[] args) throws Exception {
        QuicServerCodecBuilder serverCodecBuilder = new QuicServerCodecBuilder()
            .certificate(SelfSignedCertificate.generate())
            .handler(new ChannelInitializer<Channel>() {
                @Override
                protected void initChannel(Channel ch) {
                    ch.pipeline().addLast(new Http3ServerConnectionHandler());
                    ch.pipeline().addLast(new SimpleChannelInboundHandler<Http3HeadersFrame>() {
                        @Override
                        protected void channelRead0(ChannelHandlerContext ctx, Http3HeadersFrame frame) {
                            ctx.writeAndFlush(new DefaultHttp3HeadersFrame()
                                .status(HttpResponseStatus.OK)
                                .content(Unpooled.wrappedBuffer("Hello HTTP/3!".getBytes())));
                        }
                    });
                }
            });

        serverCodecBuilder.bind(8443).sync().channel().closeFuture().sync();
    }
}

// Example 2: REST API Server
import io.netty.incubator.codec.http3.*;
import io.netty.incubator.codec.quic.*;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import com.fasterxml.jackson.databind.ObjectMapper;

public class RestApiHttp3Server {
    private final ObjectMapper mapper = new ObjectMapper();

    class UserHandler extends SimpleChannelInboundHandler<Http3HeadersFrame> {
        @Override
        protected void channelRead0(ChannelHandlerContext ctx, Http3HeadersFrame frame) {
            if (frame.headers().method().equals(HttpMethod.GET)) {
                User user = new User("john", "doe");
                byte[] response = mapper.writeValueAsBytes(user);

                ctx.writeAndFlush(new DefaultHttp3HeadersFrame()
                    .status(HttpResponseStatus.OK)
                    .content(Unpooled.wrappedBuffer(response))
                    .contentType("application/json"));
            }
        }
    }

    public void start() throws Exception {
        QuicServerCodecBuilder.forServer()
            .handler(new ChannelInitializer<Channel>() {
                @Override
                protected void initChannel(Channel ch) {
                    ch.pipeline().addLast(new Http3ServerConnectionHandler());
                    ch.pipeline().addLast(new UserHandler());
                }
            })
            .bind(8443).sync();
    }
}

// Example 3: Streaming HTTP/3 Server
import io.netty.incubator.codec.http3.*;
import io.netty.incubator.codec.quic.*;
import io.netty.channel.*;
import reactor.core.publisher.Flux;

public class StreamingHttp3Server {
    public static void main(String[] args) throws Exception {
        QuicServerCodecBuilder.forServer()
            .handler(new ChannelInitializer<Channel>() {
                @Override
                protected void initChannel(Channel ch) {
                    ch.pipeline().addLast(new Http3ServerConnectionHandler());
                    ch.pipeline().addLast(new SimpleChannelInboundHandler<Http3RequestStreamFrame>() {
                        @Override
                        protected void channelRead0(ChannelHandlerContext ctx, Http3RequestStreamFrame frame) {
                            if (frame instanceof Http3HeadersFrame) {
                                // Start streaming response
                                Flux.interval(Duration.ofSeconds(1))
                                    .map(i -> new DefaultHttp3DataFrame(
                                        Unpooled.wrappedBuffer(("Data chunk " + i).getBytes())))
                                    .subscribe(dataFrame -> ctx.writeAndFlush(dataFrame));
                            }
                        }
                    });
                }
            })
            .bind(8443).sync();
    }
}

<!-- POM.xml dependencies -->
<dependencies>
    <dependency>
        <groupId>io.netty.incubator</groupId>
        <artifactId>netty-incubator-codec-http3</artifactId>
        <version>0.0.17.Final</version>
    </dependency>
    <dependency>
        <groupId>io.netty.incubator</groupId>
        <artifactId>netty-incubator-codec-quic</artifactId>
        <version>0.0.39.Final</version>
    </dependency>
    <dependency>
        <groupId>com.fasterxml.jackson.core</groupId>
        <artifactId>jackson-databind</artifactId>
        <version>2.14.2</version>
    </dependency>
    <dependency>
        <groupId>io.projectreactor</groupId>
        <artifactId>reactor-core</artifactId>
        <version>3.5.3</version>
    </dependency>
</dependencies>