package example;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.incubator.codec.http3.Http3Headers;
import io.netty.incubator.codec.http3.Http3ServerCodec;
import io.netty.incubator.codec.http3.Http3ServerConnectionHandler;
import io.netty.incubator.codec.quic.QuicChannel;
import io.netty.incubator.codec.quic.QuicServerCodecBuilder;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

public class Http3FortuneServer {

  private static final int PORT = 443;
  private static final String ALT_SVC = "h3=\":443\"; ma=2592000";

  private static List<String> fortunes = new ArrayList<>();

  static {
    fortunes.add("Today is your lucky day!");
    fortunes.add("A dubious friend may be an enemy in camouflage");
    fortunes.add(
      "The early bird gets the worm, but the second mouse gets the cheese"
    );
  }

  public static void main(String[] args) throws Exception {
    Vertx vertx = Vertx.vertx();
    Router router = Router.router(vertx);

    // Set up Vertx route
    router
      .get("/fortune")
      .handler(ctx -> {
        String randomFortune = fortunes.get(
          (int) (Math.random() * fortunes.size())
        );
        JsonObject response = new JsonObject().put("fortune", randomFortune);
        ctx
          .response()
          .putHeader("content-type", "application/json")
          .end(response.encode());
      });

    // Create the Netty HTTP/3 server
    QuicServerCodecBuilder.create()
      .certificateChain("path/to/cert.pem")
      .privateKey("path/to/key.pem")
      .handler(
        new Http3ServerConnectionHandler() {
          @Override
          protected ChannelHandler newRequestStreamHandler() {
            return new SimpleChannelInboundHandler<Http3Headers>() {
              @Override
              protected void channelRead0(
                ChannelHandlerContext ctx,
                Http3Headers headers
              ) {
                // Convert Netty request to Vertx request
                HttpServerRequest vertxRequest =
                  new VertxHttpServerRequestAdapter(headers);

                // Handle with router
                router.handle(vertxRequest);

                // Add Alt-Svc header
                Http3Headers responseHeaders = headers
                  .scheme("https")
                  .status("200")
                  .add("alt-svc", ALT_SVC);

                ctx.write(responseHeaders);
                ctx.flush();
              }
            };
          }
        }
      )
      .bind(new InetSocketAddress(PORT))
      .sync()
      .channel();

    System.out.println("HTTP/3 Server started on port " + PORT);
  }

  // Adapter class to convert between Netty and Vertx requests
  static class VertxHttpServerRequestAdapter implements HttpServerRequest {

    private final Http3Headers headers;

    public VertxHttpServerRequestAdapter(Http3Headers headers) {
      this.headers = headers;
    }

    // Implement HttpServerRequest methods
    @Override
    public String path() {
      return headers.path().toString();
    }

    @Override
    public String method() {
      return headers.method().toString();
    }
    // Other required implementations...
  }
}
