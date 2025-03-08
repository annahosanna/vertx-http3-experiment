package example;

import io.netty.incubator.codec.http3.Http3ServerCodec;
import io.netty.incubator.codec.quic.QuicChannel;
import io.netty.incubator.codec.quic.QuicServerCodecBuilder;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;

// Might work but does not use a vertx router
public class Http3QuicServer extends AbstractVerticle {

  private HttpServer server;

  @Override
  public void start() {
    HttpServerOptions options = new HttpServerOptions()
      .setPort(8443)
      .setSsl(true)
      .setUseAlpn(true)
      .setHost("localhost");

    QuicServerCodecBuilder quicCodec = QuicServerCodecBuilder.create()
      .sslContext(options.getSslOptions().getSslContext())
      .maxIdleTimeout(5000, TimeUnit.MILLISECONDS)
      .initialMaxData(10000000)
      .initialMaxStreamDataBidirectionalLocal(1000000)
      .initialMaxStreamDataBidirectionalRemote(1000000);

    server = vertx
      .createHttpServer(options)
      .connectionHandler(conn -> {
        QuicChannel quicChannel = conn
          .nettyChannel()
          .pipeline()
          .get(QuicChannel.class);

        if (quicChannel != null) {
          quicChannel
            .pipeline()
            .addLast(new Http3ServerCodec())
            .addLast(new Http3RequestHandler());
        }
      })
      .requestHandler(req -> {
        req
          .response()
          .putHeader("content-type", "text/plain")
          .end("Hello from HTTP/3 server!");
      })
      .listen(ar -> {
        if (ar.succeeded()) {
          System.out.println(
            "HTTP/3 server started on port " + ar.result().actualPort()
          );
        } else {
          System.err.println("Failed to start server: " + ar.cause());
        }
      });
  }

  @Override
  public void stop() {
    if (server != null) {
      server.close();
    }
  }

  public static void main(String[] args) {
    Vertx vertx = Vertx.vertx();
    vertx.deployVerticle(new Http3QuicServer());
  }
}
