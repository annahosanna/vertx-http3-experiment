// Fortune.java
public class Fortune {
    private String message;

    public Fortune(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}

// FortuneHandler.java
public class FortuneHandler {
    private List<Fortune> fortunes = new ArrayList<>();
    private ObjectMapper mapper = new ObjectMapper();

    public Mono<ServerResponse> addFortune(ServerRequest request) {
        return request.bodyToMono(Fortune.class)
            .map(fortune -> {
                fortunes.add(fortune);
                return ServerResponse.ok().build();
            });
    }

    public Mono<ServerResponse> getFortune(ServerRequest request) {
        if (fortunes.isEmpty()) {
            return ServerResponse.notFound().build();
        }

        Random rand = new Random();
        Fortune fortune = fortunes.get(rand.nextInt(fortunes.size()));
        return ServerResponse.ok()
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(fortune);
    }
}

// Http3Server.java
public class Http3Server {
    private static final int PORT = 8443;

    public static void main(String[] args) throws Exception {
        FortuneHandler fortuneHandler = new FortuneHandler();

        SslContext sslContext = SslContextBuilder.forServer(...)
            .applicationProtocolConfig(new ApplicationProtocolConfig(
                Protocol.ALPN,
                SelectorFailureBehavior.NO_ADVERTISE,
                SelectedListenerFailureBehavior.ACCEPT,
                ApplicationProtocolNames.HTTP_3))
            .build();

        QuicServerCodecBuilder serverCodecBuilder = new QuicServerCodecBuilder()
            .sslContext(sslContext)
            .maxIdleTimeout(5000, TimeUnit.MILLISECONDS)
            .initialMaxData(10000000)
            .initialMaxStreamDataBidirectionalLocal(1000000)
            .initialMaxStreamDataBidirectionalRemote(1000000)
            .initialMaxStreamsBidirectional(100);

        NioEventLoopGroup group = new NioEventLoopGroup(1);

        try {
            Bootstrap bs = new Bootstrap();
            Channel channel = bs.group(group)
                .channel(NioDatagramChannel.class)
                .handler(serverCodecBuilder.build())
                .bind(PORT).sync().channel();

            channel.closeFuture().addListener(future -> {
                group.shutdownGracefully();
            });

            RouterFunction<ServerResponse> router = RouterFunctions
                .route(POST("/fortune"), fortuneHandler::addFortune)
                .andRoute(GET("/fortune"), fortuneHandler::getFortune)
                .filter((request, next) -> {
                    if (!request.path().startsWith("/fortune")) {
                        return ServerResponse.notFound().build();
                    }
                    return next.handle(request);
                });

            HttpServer.create()
                .protocol(HttpProtocol.H3)
                .handle(router)
                .bindNow();

            System.out.println("HTTP/3 Server started on port " + PORT);
            channel.closeFuture().sync();

        } finally {
            group.shutdownGracefully();
        }
    }
}