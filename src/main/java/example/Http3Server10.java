import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.PemKeyCertOptions;

public class FortuneServer extends AbstractVerticle {

    private static final int PORT = 8443;
    private static final String[] FORTUNES = {
        "A journey of a thousand miles begins with a single step",
        "Good things come to those who wait",
        "Fortune favors the bold"
    };

    @Override
    public void start(Promise<Void> startPromise) {
        // Configure TLS options
        PemKeyCertOptions pemKeyCertOptions = new PemKeyCertOptions()
            .setKeyPath("server-key.pem")
            .setCertPath("server-cert.pem");

        // Create HTTP/2 server
        HttpServerOptions http2Options = new HttpServerOptions()
            .setUseAlpn(true)
            .setSsl(true)
            .setPemKeyCertOptions(pemKeyCertOptions)
            .setPort(PORT);

        // Create HTTP/3 server  
        HttpServerOptions http3Options = new HttpServerOptions()
            .setUseAlpn(true)
            .setSsl(true)
            .setPemKeyCertOptions(pemKeyCertOptions) 
            .setPort(PORT)
            .setQuic(true);

        vertx.createHttpServer(http2Options)
            .requestHandler(req -> {
                String fortune = FORTUNES[(int)(Math.random() * FORTUNES.length)];
                JsonObject response = new JsonObject()
                    .put("fortune", fortune);
                req.response()
                    .putHeader("content-type", "application/json")
                    .end(response.encode());
            })
            .listen()
            .compose(http2 -> {
                return vertx.createHttpServer(http3Options)
                    .requestHandler(req -> {
                        String fortune = FORTUNES[(int)(Math.random() * FORTUNES.length)];
                        JsonObject response = new JsonObject()
                            .put("fortune", fortune);
                        req.response()
                            .putHeader("content-type", "application/json")
                            .end(response.encode());
                    })
                    .listen();
            })
            .onSuccess(v -> startPromise.complete())
            .onFailure(startPromise::fail);
    }

    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(new FortuneServer());
    }
}