public class RouterToMap {

  public static Map<String, String> convertRouterToMap(Router router) {
    Map<String, String> routeMap = new HashMap<>();

    for (Route route : router.getRoutes()) {
      String path = route.getPath();
      String method = route
        .methods()
        .stream()
        .map(HttpMethod::name)
        .collect(Collectors.joining(","));

      routeMap.put(path, method);
    }

    return routeMap;
  }
}
