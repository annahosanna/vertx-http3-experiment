
public class RouterJsonConverter {

    public static JsonObject convertRouterToJson(Router router) {
        JsonObject json = new JsonObject();
        JsonArray routes = new JsonArray();

        // Get all registered routes
        router.getRoutes().forEach(route -> {
            JsonObject routeJson = new JsonObject()
                .put("path", route.getPath())
                .put("method", route.methods().toString())
                .put("regex", route.getPath() != null ? route.getPath().toString() : null)
                .put("order", route.getOrder())
                .put("enabled", route.isEnabled());

            if (route.getName() != null) {
                routeJson.put("name", route.getName());
            }

            routes.add(routeJson);
        });

        json.put("routes", routes);
        return json;
    }

}