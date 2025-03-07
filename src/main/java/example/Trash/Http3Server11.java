import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.PemKeyCertOptions;
import io.vertx.ext.web.Router;

public class FortuneServer extends AbstractVerticle {

  private static final String[] FORTUNES = {
    "The future belongs to those who believe in the beauty of their dreams.",
    "Fortune favors the bold.",
    "Take the first step in faith. You don't have to see the whole staircase.",
    "Life is what happens while you're busy making other plans."
  };

  @Override
  public void start() {
    Router router = Router.router(vertx);

    // Shared route handler for both servers
    router.get("/fortune").handler(ctx -> {
      String randomFortune = FORTUNES[(int)(Math.random() * FORTUNES.length)];
      JsonObject response = new JsonObject()
        .put("fortune", randomFortune);

      ctx.response()
        .putHeader("content-type", "application/json")
        .end(response.encode());
    });

    // HTTP/2 server setup
    HttpServerOptions http2Options = new HttpServerOptions()
      .setUseAlpn(true)
      .setSsl(true)
      .setKeyCertOptions(new PemKeyCertOptions()
        .setKeyPath("server-key.pem")
        .setCertPath("server-cert.pem"))
      .setPort(8443);

    vertx.createHttpServer(http2Options)
      .requestHandler(router)
      .connectionHandler(conn -> {
        conn.response().putHeader("Alt-Svc", "h3=\":8444\"");
      })
      .listen();

    // HTTP/3 server setup  
    HttpServerOptions http3Options = new HttpServerOptions()
      .setUseAlpn(true)
      .setSsl(true)
      .setKeyCertOptions(new PemKeyCertOptions()
        .setKeyPath("server-key.pem")
        .setCertPath("server-cert.pem"))
      .setPort(8444)
      .setQuic(true);

    vertx.createHttpServer(http3Options)
      .requestHandler(router)
      .listen();
  }

  public static void main(String[] args) {
    Vertx vertx = Vertx.vertx();
    vertx.deployVerticle(new FortuneServer());
  }
}