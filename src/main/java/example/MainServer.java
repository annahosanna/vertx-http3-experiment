package example;

import io.netty.handler.ssl.ApplicationProtocolConfig;
import io.netty.handler.ssl.ApplicationProtocolNames;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.util.SelfSignedCertificate;
// Required Imports
import io.netty.incubator.codec.http3.Http3ConnectionHandler;
import io.netty.incubator.codec.http3.Http3HeadersFrame;
import io.netty.incubator.codec.quic.QuicSslContext;
import io.netty.incubator.codec.http3.Http3ServerConnectionHandler;
import reactor.core.publisher.Mono;
import reactor.netty.incubator.quic.QuicServer;
import io.netty.incubator.codec.quic.QuicServerCodecBuilder;

class MainServer {

  public Mono<Void> start() {
	  SelfSignedCertificate ssc = new SelfSignedCertificate();
      QuicSslContext context = QuicServerCodecBuilder.configure(ssc.certificate(), ssc.privateKey())
              .build();
    SslContext sslContext = SslContextBuilder.forServer(certFile, keyFile)
      .applicationProtocolConfig(
        new ApplicationProtocolConfig(
          ApplicationProtocolConfig.Protocol.ALPN,
          ApplicationProtocolConfig.SelectorFailureBehavior.NO_ADVERTISE,
          ApplicationProtocolConfig.SelectedListenerFailureBehavior.ACCEPT
        )
      )
      .build();

    
    QuicServer quicServer = QuicServer.create()
      .secure(context)
      .port(8443)
      .wiretap(true);

    return quicServer
      .bind()
      .handle((conn, prop) ->
        conn
          // .addHandlerLast("http3Handler", new Http3ServerConnectionHandler())
          .addHandlerLast("fortuneHandler", new FortuneHeaderFrameHandler())
      );
  }
}
