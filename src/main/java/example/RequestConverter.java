package example;

import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.impl.RoutingContextImpl;
import io.vertx.ext.web.impl.RoutingContextInternal;

public class RequestConverter {
    public static RoutingContext toRoutingContext(HttpServerRequest request) {
        // Create new RoutingContext from the HttpServerRequest
        RoutingContext routingContext = new RoutingContextImpl(null, null, request, null);

        // Cast to internal interface to access additional methods if needed
        RoutingContextInternal internal = (RoutingContextInternal) routingContext;

        return routingContext;
    }
}