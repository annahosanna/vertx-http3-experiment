import io.netty.incubator.codec.http3.Http3;
import io.netty.incubator.codec.http3.Http3ServerBuilder;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;

public class Http3FortunesServer {

    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        Router router = Router.router(vertx);

        // Setup fortune endpoint
        router.get("/fortunes").handler(ctx -> {
            JsonArray fortunes = new JsonArray()
                .add(new JsonObject()
                    .put("id", 1)
                    .put("message", "A journey of a thousand miles begins with a single step"))
                .add(new JsonObject()
                    .put("id", 2) 
                    .put("message", "Fortune favors the bold"));

            ctx.response()
                .putHeader("content-type", "application/json")
                .end(fortunes.encode());
        });

        // Configure HTTP/3 server
        Http3ServerBuilder.create()
            .host("localhost")
            .port(8443)
            .handler(router::handle)
            .build()
            .listen()
            .thenAccept(server -> {
                System.out.println("HTTP/3 server started on port 8443");
            })
            .exceptionally(t -> {
                t.printStackTrace();
                return null;
            });
    }
}