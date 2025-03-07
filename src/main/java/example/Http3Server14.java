import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.Http3Options;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.net.JksOptions;
import io.vertx.ext.web.Router;

public class Http3Server {

    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx(new VertxOptions()
            .setPreferNativeTransport(true));

        Router router = Router.router(vertx);

        router.get("/").handler(ctx -> {
            ctx.response()
                .putHeader("content-type", "text/plain")
                .end("Hello from HTTP/3 server!");
        });

        Http3Options http3Options = new Http3Options()
            .setInitialMaxStreamDataBidirectionalLocal(1048576)
            .setInitialMaxStreamDataBidirectionalRemote(1048576);

        HttpServerOptions serverOptions = new HttpServerOptions()
            .setUseAlpn(true)
            .setHttp3(http3Options)
            .setSsl(true)
            .setKeyCertOptions(new JksOptions()
                .setPath("server-keystore.jks")
                .setPassword("password"));

        vertx.createHttpServer(serverOptions)
            .requestHandler(router)
            .listen(8443)
            .onSuccess(server -> {
                System.out.println("HTTP/3 server started on port " + server.actualPort());
            })
            .onFailure(error -> {
                System.out.println("Failed to start server: " + error.getMessage());
            });
    }
}