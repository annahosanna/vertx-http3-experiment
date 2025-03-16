class Fortune {
  private Router router;
  private final String[] fortunes = {
    "A journey of a thousand miles begins with a single step",
    "Fortune favors the bold",
    "You will find happiness in unexpected places",
    "Good things come to those who wait",
    "Today is your lucky day"
  };

  public Fortune(Router router) {
    this.router = router;
    setupRoutes();
  }

  private void setupRoutes() {
    router.get("/fortune").handler(this::getFortune);
  }

  private void getFortune(RoutingContext ctx) {
    Random rand = new Random();
    String fortune = fortunes[rand.nextInt(fortunes.length)];
    ctx.response()
       .putHeader("content-type", "text/plain")
       .end(fortune);
  }
}