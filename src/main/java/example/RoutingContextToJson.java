public class RoutingContextHandler {
    public static JsonObject toJson(RoutingContext context) {
        JsonObject json = new JsonObject();

        // Add request details
        json.put("method", context.request().method().toString());
        json.put("path", context.request().path());
        json.put("uri", context.request().absoluteURI());

        // Add headers
        JsonObject headers = new JsonObject();
        context.request().headers().forEach(header -> {
            headers.put(header.getKey(), header.getValue());
        });
        json.put("headers", headers);

        // Add query parameters
        JsonObject params = new JsonObject();
        context.queryParams().forEach(param -> {
            params.put(param.getKey(), param.getValue());
        });
        json.put("params", params);

        // Add body if present
        if (context.getBody() != null) {
            json.put("body", context.getBodyAsString());
        }

        return json;
    }
}