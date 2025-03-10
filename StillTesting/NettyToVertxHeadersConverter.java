package example;

import io.netty.handler.codec.http.HttpHeaders;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpServerRequest;

public class NettyToVertxHeadersConverter {

  public static void convertHeaders(
    HttpHeaders nettyHeaders,
    HttpServerRequest vertxRequest
  ) {
    MultiMap vertxHeaders = vertxRequest.headers();

    // Iterate through Netty headers and add to Vert.x headers
    nettyHeaders
      .entries()
      .forEach(entry -> {
        String name = entry.getKey();
        String value = entry.getValue();
        vertxHeaders.add(name, value);
      });
  }

  public static void main(String[] args) {
    // Example usage
    HttpHeaders nettyHeaders = new DefaultHttpHeaders();
    nettyHeaders.add("Content-Type", "application/json");
    nettyHeaders.add("Authorization", "Bearer token123");

    // Create Vert.x request (this would normally come from your Vert.x server)
    HttpServerRequest vertxRequest = // your vertx request instance
      // Convert headers
      convertHeaders(nettyHeaders, vertxRequest);
  }
}
