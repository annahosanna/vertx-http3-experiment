import io.netty.incubator.codec.http3.*;
import io.netty.handler.codec.http.*;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.util.CharsetUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class Http3Server {
    private final List<Fortune> fortunes = new ArrayList<>();
    private final ObjectMapper mapper = new ObjectMapper();

    public static void main(String[] args) throws Exception {
        new Http3Server().start();
    }

    public void start() throws Exception {
        QuicServerCodecBuilder.forServer()
            .sslContext(SslContextBuilder.forServer(...).build())
            .handler(new ChannelInitializer<Channel>() {
                @Override
                protected void initChannel(Channel ch) {
                    ch.pipeline()
                        .addLast(new Http3ServerConnectionHandler())
                        .addLast(new FortuneHandler(fortunes, mapper));
                }
            })
            .bind(8443)
            .sync()
            .channel()
            .closeFuture()
            .sync();
    }
}

class FortuneHandler extends SimpleChannelInboundHandler<Http3HeadersFrame> {
    private final List<Fortune> fortunes;
    private final ObjectMapper mapper;

    public FortuneHandler(List<Fortune> fortunes, ObjectMapper mapper) {
        this.fortunes = fortunes;
        this.mapper = mapper;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Http3HeadersFrame frame) throws Exception {
        String path = frame.headers().path().toString();

        if (path.equals("/fortune")) {
            if (frame.method().equals(HttpMethod.POST)) {
                handleAddFortune(ctx, frame);
            } else if (frame.method().equals(HttpMethod.GET)) {
                handleGetFortune(ctx);
            } else {
                send404(ctx);
            }
        } else {
            send404(ctx);
        }
    }

    private void handleAddFortune(ChannelHandlerContext ctx, Http3HeadersFrame frame) {
        ctx.channel().read().addListener(new FortuneAddFuture(ctx, fortunes, mapper));
    }

    private void handleGetFortune(ChannelHandlerContext ctx) throws Exception {
        if (fortunes.isEmpty()) {
            sendResponse(ctx, "No fortunes available", HttpResponseStatus.NOT_FOUND);
            return;
        }

        Random rand = new Random();
        Fortune fortune = fortunes.get(rand.nextInt(fortunes.size()));
        String jsonFortune = mapper.writeValueAsString(fortune);
        sendResponse(ctx, jsonFortune, HttpResponseStatus.OK);
    }

    private void send404(ChannelHandlerContext ctx) {
        sendResponse(ctx, "Not Found", HttpResponseStatus.NOT_FOUND);
    }

    private void sendResponse(ChannelHandlerContext ctx, String message, HttpResponseStatus status) {
        ByteBuf content = Unpooled.copiedBuffer(message, CharsetUtil.UTF_8);
        Http3Headers headers = Http3Headers.newHeaders()
            .status(status.codeAsText())
            .contentType("application/json")
            .contentLength(content.readableBytes());

        ctx.write(new DefaultHttp3HeadersFrame(headers));
        ctx.writeAndFlush(new DefaultHttp3DataFrame(content));
    }
}

class FortuneAddFuture implements ChannelFutureListener {
    private final ChannelHandlerContext ctx;
    private final List<Fortune> fortunes;
    private final ObjectMapper mapper;

    public FortuneAddFuture(ChannelHandlerContext ctx, List<Fortune> fortunes, ObjectMapper mapper) {
        this.ctx = ctx;
        this.fortunes = fortunes;
        this.mapper = mapper;
    }

    @Override
    public void operationComplete(ChannelFuture future) throws Exception {
        if (future.isSuccess()) {
            Http3DataFrame data = (Http3DataFrame) future.get();
            String content = data.content().toString(CharsetUtil.UTF_8);
            Fortune fortune = mapper.readValue(content, Fortune.class);
            fortunes.add(fortune);
            sendResponse(ctx, "Fortune added successfully", HttpResponseStatus.CREATED);
        } else {
            sendResponse(ctx, "Error processing request", HttpResponseStatus.BAD_REQUEST);
        }
    }

    private void sendResponse(ChannelHandlerContext ctx, String message, HttpResponseStatus status) {
        ByteBuf content = Unpooled.copiedBuffer(message, CharsetUtil.UTF_8);
        Http3Headers headers = Http3Headers.newHeaders()
            .status(status.codeAsText())
            .contentType("application/json")
            .contentLength(content.readableBytes());

        ctx.write(new DefaultHttp3HeadersFrame(headers));
        ctx.writeAndFlush(new DefaultHttp3DataFrame(content));
    }
}

class Fortune {
    private String message;

    public Fortune() {}

    public Fortune(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}