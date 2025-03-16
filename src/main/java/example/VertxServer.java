package example;

import io.vertx.core.Future;
import io.vertx.core.VerticleBase;
import io.vertx.ext.web.Router;
import io.vertx.launcher.application.VertxApplication;

/*
 * @author <a href="http://tfox.org">Tim Fox</a>
 */
public class VertxServer extends VerticleBase {

  public static void main(String[] args) {
    VertxApplication.main(new String[] { Server.class.getName() });
  }

  @Override
  public Future<?> start() throws Exception {
    Router router = Router.router(vertx);

    String responseText = new String("Just a test");
    router
      .route()
      .handler(routingContext -> {
        routingContext
          .response()
          .putHeader("Content-Type", "text/plain")
          .putHeader("Content-Length", responseText.length)
          .putHeader("Alt-Svc", "h3=":8443")
          .end(responseText);
      });

    return vertx.createHttpServer().requestHandler(router).listen(8080);
  }
}
