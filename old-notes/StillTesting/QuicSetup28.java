QuicServer quicServer = new QuicServer();
quicServer.bind()
    .doOnConnection(quicConnection -> {
        ChannelPipeline pipeline = quicConnection.channel().pipeline(); 
        pipeline.addLast(new ChannelHandler() {
            @Override
            public void channelActive(ChannelHandlerContext ctx) {
                // Handle new connection
            }

            @Override 
            public void channelRead(ChannelHandlerContext ctx, Object msg) {
                // Handle incoming data
            }
        });
    })
    .subscribe();