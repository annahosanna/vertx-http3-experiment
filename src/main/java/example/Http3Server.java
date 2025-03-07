import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.net.PemKeyCertOptions;
import io.vertx.ext.web.Router;

public class Http3Server extends AbstractVerticle {

    @Override
    public void start(Promise<Void> startPromise) {
        Router router = Router.router(vertx);

        // Add routes
        router.get("/").handler(ctx -> {
            ctx.response().end("Hello from HTTP/3 server!");
        });

        // Configure HTTP/3 options
        HttpServerOptions options = new HttpServerOptions()
            .setUseAlpn(true)
            .setHttpVersion(HttpVersion.HTTP_3)
            .setSsl(true)
            .setPemKeyCertOptions(new PemKeyCertOptions()
                .setKeyPath("server-key.pem")
                .setCertPath("server-cert.pem"));

        // Create HTTP/3 server
        vertx.createHttpServer(options)
            .requestHandler(router)
            .listen(8443, ar -> {
                if (ar.succeeded()) {
                    System.out.println("HTTP/3 server started on port 8443");
                    startPromise.complete();
                } else {
                    System.out.println("Failed to start HTTP/3 server: " + ar.cause());
                    startPromise.fail(ar.cause());
                }
            });
    }

    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(new Http3Server());
    }
}