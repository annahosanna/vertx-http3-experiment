package example;

import io.netty.handler.ssl.ApplicationProtocolConfig;
import io.netty.handler.ssl.SslContextBuilder;
// Required Imports
import io.netty.incubator.codec.http3.Http3HeadersFrame;
import io.netty.incubator.codec.http3.Http3ServerConnectionHandler;
import reactor.core.publisher.Mono;
import reactor.netty.quic.QuicServer;

class MainServer {

  public Mono<Void> start() {
    SslContext sslContext = SslContextBuilder.forServer(certFile, keyFile)
      .applicationProtocolConfig(
        new ApplicationProtocolConfig(
          ApplicationProtocolConfig.Protocol.ALPN,
          ApplicationProtocolConfig.SelectorFailureBehavior.NO_ADVERTISE,
          ApplicationProtocolConfig.SelectedListenerFailureBehavior.ACCEPT,
          ApplicationProtocolNames.HTTP_3
        )
      )
      .build();

    QuicServer quicServer = QuicServer.create()
      .secure(sslContext)
      .port(8443)
      .wiretap(true);

    return quicServer
      .bind()
      .doOnConnection(conn ->
        conn
          .addHandlerLast("http3Handler", new Http3ServerConnectionHandler())
          .addHandlerLast("fortuneHandler", new FortuneHeaderFrameHandler())
      );
  }
}
