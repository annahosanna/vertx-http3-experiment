public class RouterToArray {

    public static Object[] convertRouterToArray(Router router) {
        List<Map<String, String>> routesList = new ArrayList<>();

        router.getRoutes().forEach(route -> {
            Map<String, String> routeInfo = new HashMap<>();
            routeInfo.put("path", route.getPath());
            routeInfo.put("method", route.getMethod().toString());
            routesList.add(routeInfo);
        });

        return routesList.toArray();
    }

    public static String convertToJson(Router router) {
        Object[] routes = convertRouterToArray(router);
        return new JsonArray(Arrays.asList(routes)).encode();
    }
}