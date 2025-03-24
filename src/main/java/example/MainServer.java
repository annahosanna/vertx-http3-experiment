package example;

import io.netty.incubator.codec.quic.QuicServerCodecBuilder;
// import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
// import io.netty.handler.ssl.util.SimpleTrustManagerFactory;
// import io.netty.handler.ssl.ApplicationProtocolConfig;
// import io.netty.handler.ssl.ApplicationProtocolNames;
// import io.netty.handler.ssl.SslContextBuilder;
// import io.netty.handler.ssl.SslContext;
// import io.netty.handler.ssl.util.SelfSignedCertificate;
// Required Imports
// import io.netty.incubator.codec.http3.Http3ConnectionHandler;
// import io.netty.incubator.codec.http3.Http3HeadersFrame;
import io.netty.incubator.codec.quic.QuicSslContext;
import io.netty.incubator.codec.quic.QuicSslContextBuilder;
import java.io.File;
import java.net.InetSocketAddress;
// import java.security.cert.CertificateException;
// import javax.net.ssl.TrustManagerFactory;
import reactor.core.publisher.Mono;
// import io.netty.incubator.codec.quic.QuicTokenHandler;
// import io.netty.incubator.codec.http3.*;
// import io.netty.incubator.codec.http3.Http3ServerConnectionHandler;
// import io.netty.incubator.codec.http3.Http3ConnectionHandler;
// import io.netty.incubator.codec.http3.Http3ServerConnectionHandler;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;
import reactor.netty.incubator.quic.QuicServer;
import io.netty.channel.ChannelHandler;
import java.util.concurrent.atomic.*;

class MainServer {


  public static void main(String[] args) {
    // x509 cert file in pem format and pkcs8 private key pem format
    // Public: Pem encoded x509 certificate chain
    // Private PKCS* pem
    File keyFile = new File("key.pem");
    File certsFile = new File("certs.pem");
    QuicSslContext context = QuicSslContextBuilder
    		.forServer(
      keyFile,
      null,
      certsFile
    )
      .applicationProtocols("h3") // ALPN protocol for HTTP/3
      .build();

    // Add this to the channel pipeline
    ChannelHandler codecChannelHandler = new QuicServerCodecBuilder()
    		.sslContext(context)
    		.handler(new FortuneHeaderFrameHandler())
    		.streamHandler(new Http3FortuneStreamHandler())
    		.tokenHandler(new QuicTokenEncryptionHandler())
    		.build();
    // tokenHandler MUST be defined here
    QuicServer quicServer = QuicServer.create()
      .tokenHandler(new QuicTokenEncryptionHandler())
      .bindAddress(() -> new InetSocketAddress("localhost", 8443))
      .secure(context)
      .handleStream((in, out) -> {
        in.withConnection(conn ->
          conn.addHandlerLast(new Http3FortuneStreamHandler())
        );
        return out.sendString(Mono.empty());
      })
      .wiretap(true);

    try {
      quicServer
        .bindNow()
        .addHandlerLast(codecChannelHandler)
        .bind()
        .onDispose()
        .block();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
