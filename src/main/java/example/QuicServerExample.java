package example;

import io.netty.handler.ssl.ApplicationProtocolConfig;
import io.netty.handler.ssl.ApplicationProtocolNames;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.incubator.codec.quic.QuicSslContext;
import io.netty.incubator.codec.quic.QuicStreamType;
import reactor.core.publisher.Mono;
import reactor.netty.quic.QuicServer;

public class QuicServerExample {

  public static void main(String[] args) {
    QuicSslContext sslContext = null;
    try {
      sslContext = QuicSslContext.buildForServer(
        SslContextBuilder.forServer("cert.pem", "key.pem")
          .applicationProtocolConfig(
            new ApplicationProtocolConfig(
              ApplicationProtocolConfig.Protocol.ALPN,
              ApplicationProtocolConfig.SelectorFailureBehavior.NO_ADVERTISE,
              ApplicationProtocolConfig.SelectedListenerFailureBehavior.ACCEPT,
              ApplicationProtocolNames.HTTP_1_1
            )
          )
          .build()
      );
    } catch (Exception e) {
      e.printStackTrace();
    }

    final QuicSslContext finalSslContext = sslContext;

    QuicServer.create()
      .port(8443)
      .secure(spec -> spec.sslContext(finalSslContext))
      .handle((in, out) -> {
        return out.sendString(Mono.just("Hello from QUIC server!")).then();
      })
      .bindNow()
      .onDispose()
      .block();
  }
}
