import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.handler.ssl.SslContext;
import io.netty.incubator.codec.http3.*;
import io.netty.incubator.codec.quic.*;
import org.h2.jdbcx.JdbcConnectionPool;

public class FortuneServer {
    private final int port;
    private final SslContext sslContext;
    private final JdbcConnectionPool pool;

    public FortuneServer(int port, SslContext sslContext) {
        this.port = port;
        this.sslContext = sslContext;
        this.pool = JdbcConnectionPool.create("jdbc:h2:~/fortunes", "sa", "");
    }

    public void start() throws Exception {
        QuicServerCodecBuilder serverCodecBuilder = new QuicServerCodecBuilder()
            .sslContext(sslContext)
            .maxIdleTimeout(5000, TimeUnit.MILLISECONDS)
            .initialMaxData(10000000)
            .initialMaxStreamDataBidirectionalLocal(1000000)
            .initialMaxStreamDataBidirectionalRemote(1000000);

        ChannelHandler codec = serverCodecBuilder.build();

        ServerBootstrap b = new ServerBootstrap();
        b.group(new NioEventLoopGroup())
         .channel(NioServerSocketChannel.class)
         .childHandler(new ChannelInitializer<Channel>() {
             @Override
             protected void initChannel(Channel ch) {
                 ch.pipeline()
                   .addLast(codec)
                   .addLast(new FortuneServerHandler(pool));
             }
         });

        Channel channel = b.bind(port).sync().channel();
        channel.closeFuture().sync();
    }
}

class FortuneServerHandler extends SimpleChannelInboundHandler<Http3HeadersFrame> {
    private final JdbcConnectionPool pool;

    public FortuneServerHandler(JdbcConnectionPool pool) {
        this.pool = pool;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Http3HeadersFrame frame) {
        String uri = frame.headers().get(HttpHeaderNames.PATH).toString();

        if ("/fortune".equals(uri) && frame.headers().method().equals(HttpMethod.GET)) {
            new GetFortuneHandler(pool).handle(ctx);
        } else if ("/fortune".equals(uri) && frame.headers().method().equals(HttpMethod.POST)) {
            new AddFortuneHandler(pool).handle(ctx);
        } else {
            sendError(ctx, HttpResponseStatus.NOT_FOUND);
        }
    }

    private void sendError(ChannelHandlerContext ctx, HttpResponseStatus status) {
        Http3Headers headers = new DefaultHttp3Headers()
            .status(status.codeAsText())
            .set(HttpHeaderNames.CONTENT_TYPE, "text/plain");

        ctx.write(new DefaultHttp3HeadersFrame(headers));
        ctx.writeAndFlush(new DefaultHttp3DataFrame(Unpooled.EMPTY_BUFFER));
    }
}

class GetFortuneHandler {
    private final JdbcConnectionPool pool;

    public GetFortuneHandler(JdbcConnectionPool pool) {
        this.pool = pool;
    }

    public void handle(ChannelHandlerContext ctx) {
        try (Connection conn = pool.getConnection()) {
            PreparedStatement stmt = conn.prepareStatement("SELECT text FROM fortunes ORDER BY RANDOM() LIMIT 1");
            ResultSet rs = stmt.executeQuery();

            String fortune = rs.next() ? rs.getString("text") : "No fortunes available";

            Http3Headers headers = new DefaultHttp3Headers()
                .status(HttpResponseStatus.OK.codeAsText())
                .set(HttpHeaderNames.CONTENT_TYPE, "text/plain");

            ctx.write(new DefaultHttp3HeadersFrame(headers));
            ctx.writeAndFlush(new DefaultHttp3DataFrame(Unpooled.copiedBuffer(fortune, CharsetUtil.UTF_8)));

        } catch (SQLException e) {
            ctx.writeAndFlush(new DefaultHttp3HeadersFrame(
                new DefaultHttp3Headers().status(HttpResponseStatus.INTERNAL_SERVER_ERROR.codeAsText())
            ));
        }
    }
}

class AddFortuneHandler {
    private final JdbcConnectionPool pool;

    public AddFortuneHandler(JdbcConnectionPool pool) {
        this.pool = pool;
    }

    public void handle(ChannelHandlerContext ctx) {
        ctx.channel().read().addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                Http3DataFrame data = (Http3DataFrame) future.get();
                String fortune = data.content().toString(CharsetUtil.UTF_8);

                try (Connection conn = pool.getConnection()) {
                    PreparedStatement stmt = conn.prepareStatement("INSERT INTO fortunes (text) VALUES (?)");
                    stmt.setString(1, fortune);
                    stmt.executeUpdate();

                    Http3Headers headers = new DefaultHttp3Headers()
                        .status(HttpResponseStatus.CREATED.codeAsText());

                    ctx.write(new DefaultHttp3HeadersFrame(headers));
                    ctx.writeAndFlush(new DefaultHttp3DataFrame(Unpooled.EMPTY_BUFFER));

                } catch (SQLException e) {
                    ctx.writeAndFlush(new DefaultHttp3HeadersFrame(
                        new DefaultHttp3Headers().status(HttpResponseStatus.INTERNAL_SERVER_ERROR.codeAsText())
                    ));
                }
            }
        });
    }
}