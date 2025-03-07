import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.http.Http3ServerOptions;
import io.vertx.core.net.PemKeyCertOptions;
import io.vertx.quic.QuicServer;
import io.vertx.quic.QuicServerOptions;

public class HTTP3Server extends AbstractVerticle {

  public static void main(String[] args) {
    Vertx vertx = Vertx.vertx();
    vertx.deployVerticle(new HTTP3Server());
  }

  @Override
  public void start() throws Exception {
    // Configure HTTP/3 server options
    QuicServerOptions options = new QuicServerOptions()
      .setPort(8443)
      .setHost("localhost")
      .setPemKeyCertOptions(new PemKeyCertOptions()
        .setKeyPath("private.pem")
        .setCertPath("cert.pem"));

    // Create and start the QUIC server
    QuicServer server = vertx.createQuicServer(options);

    server.requestHandler(request -> {
      request.response()
        .putHeader("content-type", "text/plain")
        .end("Hello from HTTP/3 server!");
    });

    server.listen(ar -> {
      if (ar.succeeded()) {
        System.out.println("HTTP/3 server started on port " + server.actualPort());
      } else {
        System.out.println("Failed to start server: " + ar.cause());
      }
    });
  }
}