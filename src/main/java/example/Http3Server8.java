import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpVersion;
import io.vertx.ext.web.Router;

public class VertxServers {
    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();

        // HTTP/3 Server
        HttpServerOptions http3Options = new HttpServerOptions()
            .setUseAlpn(true)
            .setProtocolVersion(HttpVersion.HTTP_3);

        HttpServer http3Server = vertx.createHttpServer(http3Options);
        Router http3Router = Router.router(vertx);

        http3Router.get("/fortunes").handler(ctx -> {
            ctx.response()
               .putHeader("content-type", "application/json")
               .end("{\"fortune\": \"Your future is bright!\"}");
        });

        http3Server.requestHandler(http3Router).listen(8443);

        // HTTP/2 Server
        HttpServerOptions http2Options = new HttpServerOptions()
            .setUseAlpn(true)
            .setProtocolVersion(HttpVersion.HTTP_2);

        HttpServer http2Server = vertx.createHttpServer(http2Options);
        Router http2Router = Router.router(vertx);

        http2Router.get("/fortunes").handler(ctx -> {
            ctx.response()
               .putHeader("content-type", "application/json")
               .putHeader("Alt-Svc", "h3=\":8443\"") // Advertise HTTP/3 endpoint
               .end("{\"fortune\": \"Your future is bright!\"}");
        });

        http2Server.requestHandler(http2Router).listen(8080);
    }
}