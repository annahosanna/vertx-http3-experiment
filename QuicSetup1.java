import io.netty.incubator.codec.quic.QuicServerCodecBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;

public class QuicServerExample {
    public static void main(String[] args) throws Exception {
        // Create self-signed certificate for QUIC
        SelfSignedCertificate cert = new SelfSignedCertificate();

        // Configure QUIC server codec
        QuicServerCodecBuilder codecBuilder = new QuicServerCodecBuilder()
            .certificate(cert.certificate())
            .privateKey(cert.privateKey()) 
            .localConnectionIdLength(10)
            .maxIdleTimeout(5000, TimeUnit.MILLISECONDS)
            .initialMaxData(10000000)
            .initialMaxStreamDataBidirectionalLocal(1000000)
            .initialMaxStreamDataBidirectionalRemote(1000000)
            .initialMaxStreamsBidirectional(100)
            .token(null);

        // Build the codec
        QuicServerCodec codec = codecBuilder.build();

        // Use codec in server bootstrap
        Bootstrap bootstrap = new Bootstrap()
            .group(group)
            .channel(NioDatagramChannel.class)
            .handler(codec);

        Channel channel = bootstrap.bind(8080).sync().channel();
        channel.closeFuture().sync();
    }
}