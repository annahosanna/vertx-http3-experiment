import io.netty.handler.codec.http3.Http3;
import io.netty.handler.codec.http3.Http3ServerCodecBuilder;
import io.netty.handler.ssl.SslContext;
import io.netty.incubator.codec.http3.Http3ServerBuilder;
import io.netty.incubator.codec.quic.QuicServer;

public class Http3ServerBuilder {
    private SslContext sslContext;
    private int port;
    private String host;

    public Http3ServerBuilder() {
        this.port = 443; // Default HTTPS port
        this.host = "localhost";
    }

    public Http3ServerBuilder withSslContext(SslContext sslContext) {
        this.sslContext = sslContext;
        return this;
    }

    public Http3ServerBuilder withPort(int port) {
        this.port = port;
        return this;
    }

    public Http3ServerBuilder withHost(String host) {
        this.host = host;
        return this;
    }

    public QuicServer build() {
        return Http3ServerBuilder.create()
            .sslContext(sslContext)
            .host(host)
            .port(port)
            .build();
    }
}