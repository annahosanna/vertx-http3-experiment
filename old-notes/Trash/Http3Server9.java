import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpVersion;
import io.vertx.ext.web.Router;

public class MultiVersionServer {

    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        Router router = Router.router(vertx);

        int port = 8443;

        // HTTP/3 server on UDP
        HttpServerOptions h3Options = new HttpServerOptions()
            .setPort(port)
            .setHost("localhost")
            .setUseAlpn(true)
            .setSsl(true)
            .setKeyCertOptions(/* configure SSL cert */)
            .setVersion(HttpVersion.HTTP_3);

        // HTTP/2 server on TCP    
        HttpServerOptions h2Options = new HttpServerOptions()
            .setPort(port)
            .setHost("localhost")
            .setUseAlpn(true) 
            .setSsl(true)
            .setKeyCertOptions(/* configure SSL cert */)
            .setVersion(HttpVersion.HTTP_2);

        // Add fortune route
        router.get("/fortune").handler(ctx -> {
            String[] fortunes = {
                "A beautiful, smart, and loving person will be coming into your life.",
                "Your abilities are unparalleled.",
                "You will become great if you believe in yourself.",
                "Fortune favors the bold."
            };

            String randomFortune = fortunes[(int)(Math.random() * fortunes.length)];

            ctx.response()
                .putHeader("content-type", "application/json")
                .putHeader("Alt-Svc", "h3=\":"+port+"\"; ma=86400")
                .end("{\"fortune\": \"" + randomFortune + "\"}");
        });

        // Start both servers
        vertx.createHttpServer(h3Options)
            .requestHandler(router)
            .listen();

        vertx.createHttpServer(h2Options)
            .requestHandler(router)
            .listen();
    }
}