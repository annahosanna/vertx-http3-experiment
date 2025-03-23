import io.netty.incubator.codec.http3.*;
import io.netty.channel.*;

public class Http3Server {
    public static void main(String[] args) throws Exception {
        Http3ServerBuilder builder = Http3Server.builder()
            .host("localhost") 
            .port(8443)
            .handler(new ChannelInitializer<Channel>() {
                @Override
                protected void initChannel(Channel ch) {
                    ch.pipeline().addLast(new Http3ServerHandler());
                }
            });

        Http3Server server = builder.build();
        server.start();
    }
}

public class FileUploadServer {
    public void startServer() throws Exception {
        Http3ServerBuilder builder = Http3Server.builder()
            .host("0.0.0.0")
            .port(443)
            .handler(new ChannelInitializer<Channel>() {
                @Override
                protected void initChannel(Channel ch) {
                    ChannelPipeline p = ch.pipeline();
                    p.addLast(new Http3ServerCodec());
                    p.addLast(new FileUploadHandler()); 
                }
            });

        Http3Server server = builder.build();
        server.start();
    }
}

public class StreamingServer {
    private Http3Server server;

    public void init() throws Exception {
        Http3ServerBuilder builder = Http3Server.builder()
            .host("localhost")
            .port(8080) 
            .handler(new ChannelInitializer<Channel>() {
                @Override
                protected void initChannel(Channel ch) {
                    ChannelPipeline p = ch.pipeline();
                    p.addLast(new Http3ServerCodec());
                    p.addLast(new StreamHandler());
                    p.addLast(new ChunkedWriteHandler());
                }
            });

        server = builder.build();
        server.start();
    }

    public void shutdown() {
        if (server != null) {
            server.shutdown();
        }
    }
}