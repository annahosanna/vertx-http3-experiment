class SimpleStreamHandler extends Http3RequestStreamInboundHandler {

    @Override
    protected void channelRead(ChannelHandlerContext ctx, Http3HeadersFrame frame) {
        System.out.println("Received headers: " + frame.headers());
    }

    @Override
    protected void channelRead(ChannelHandlerContext ctx, Http3DataFrame frame) {
        System.out.println("Received data frame");
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }
}



class Http3FortuneHandler extends SimpleChannelInboundHandler<Http3HeadersFrame> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Http3HeadersFrame frame) {
        String fortune = QuicFortuneServer.FORTUNES.get(
            QuicFortuneServer.RANDOM.nextInt(QuicFortuneServer.FORTUNES.size())
        );

        Http3HeadersFrame response = new DefaultHttp3HeadersFrame();
        response.headers()
            .status("200")
            .add("content-type", "text/plain");

        ctx.writeAndFlush(response);
        ctx.writeAndFlush(new Http3DataFrame(fortune));
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}

    static class Http3ServerHandler extends Http3RequestStreamInboundHandler {
        private static final String[] FORTUNES = {
            "Life is what happens while you're busy making other plans",
            "Today is the first day of the rest of your life",
            "The only constant in life is change"
        };

        @Override
        protected void channelRead(ChannelHandlerContext ctx, Http3HeadersFrame headersFrame) {
            Mono.just(FORTUNES[(int)(Math.random() * FORTUNES.length)])
                .map(fortune -> {
                    Http3Headers headers = Http3Headers.newHeaders();
                    headers.status("200");
                    headers.add("content-type", "text/plain");

                    ctx.write(new DefaultHttp3HeadersFrame(headers));
                    return fortune;
                })
                .subscribe(fortune -> {
                    ctx.writeAndFlush(new DefaultHttp3DataFrame(
                        ctx.alloc().buffer().writeBytes(fortune.getBytes())));
                });
        }
    }
}

    private static class Http3FortuneStreamHandler extends Http3RequestStreamInboundHandler {
      private static final String[] FORTUNES = {
          "Life is what happens while you're busy making other plans",
          "Today is the first day of the rest of your life",
          "The only constant in life is change"
      };

        @Override
        protected void channelRead(ChannelHandlerContext ctx, Http3HeadersFrame headersFrame,
                                 boolean isLast) throws Exception {
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
                ctx.write(new DefaultHttp3DataFrame(
                    Unpooled.wrappedBuffer(jsonResponse.getBytes())
                ));
                ctx.flush();
            } else {
                Http3Headers headers = new DefaultHttp3Headers();
                headers.status("404");
                ctx.write(new DefaultHttp3HeadersFrame(headers));
                ctx.flush();
            }
        }
    }

    private static class Fortune {
        private String message;

        public Fortune(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }
}

   private static class FortuneServerHandler extends Http3RequestStreamInboundHandler {
        @Override
        protected void channelRead(ChannelHandlerContext ctx, Http3HeadersFrame frame) throws Exception {
            String fortune = FORTUNES.get(RANDOM.nextInt(FORTUNES.size()));

            Http3Headers headers = new DefaultHttp3Headers()
                .status("200")
                .add("content-type", "text/plain");

            ctx.write(new DefaultHttp3HeadersFrame(headers));
            ctx.writeAndFlush(new DefaultHttp3DataFrame(Unpooled.copiedBuffer(fortune, CharsetUtil.UTF_8)))
                .addListener(ChannelFutureListener.CLOSE);
        }

        @Override
        protected void channelRead(ChannelHandlerContext ctx, Http3DataFrame frame) throws Exception {
            // Release the frame to prevent memory leaks
            frame.release();
        }
    }
}

    static class Http3ServerHandler extends Http3RequestStreamInboundHandler {
        private static final String[] FORTUNES = {
            "Life is what happens while you're busy making other plans",
            "Today is the first day of the rest of your life",
            "The only constant in life is change"
        };

        @Override
        protected void channelRead(ChannelHandlerContext ctx, Http3HeadersFrame headersFrame) {
            Mono.just(FORTUNES[(int)(Math.random() * FORTUNES.length)])
                .map(fortune -> {
                    Http3Headers headers = Http3Headers.newHeaders();
                    headers.status("200");
                    headers.add("content-type", "text/plain");

                    ctx.write(new DefaultHttp3HeadersFrame(headers));
                    return fortune;
                })
                .subscribe(fortune -> {
                    ctx.writeAndFlush(new DefaultHttp3DataFrame(
                        ctx.alloc().buffer().writeBytes(fortune.getBytes())));
                });
        }
    }
}
