package example;

private static class Http3FortuneStreamHandler
  extends Http3RequestStreamInboundHandler {

  private static final String[] FORTUNES = {
    "Life is what happens while you're busy making other plans",
    "Today is the first day of the rest of your life",
    "The only constant in life is change",
  };

  @Override
  protected void channelRead(
    ChannelHandlerContext ctx,
    Http3HeadersFrame headersFrame,
    boolean isLast
  ) throws Exception {
    String path = headersFrame.headers().path().toString();

    if ("/fortune".equals(path)) {
      String fortune = FORTUNES.get(RANDOM.nextInt(FORTUNES.size()));
      //String jsonResponse = MAPPER.writeValueAsString(
      String startOfString = new String("{fortune:\"");
      String endOfString = new String("\"}");
      String jsonResponse = startOfString.concat(fortune).concat(endOfString);

      Http3Headers headers = new DefaultHttp3Headers();
      headers.status("200");
      headers.add("content-type", "application/json");

      ctx.write(new DefaultHttp3HeadersFrame(headers));
      ctx.write(
        new DefaultHttp3DataFrame(
          Unpooled.wrappedBuffer(jsonResponse.getBytes())
        )
      );
      ctx.flush();
    } else {
      Http3Headers headers = new DefaultHttp3Headers();
      headers.status("404");
      ctx.write(new DefaultHttp3HeadersFrame(headers));
      ctx.flush();
    }
  }
}
