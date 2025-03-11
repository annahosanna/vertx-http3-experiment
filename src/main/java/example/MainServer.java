package example;

import java.security.cert.CertificateException;

import io.netty.handler.ssl.ApplicationProtocolConfig;
import io.netty.handler.ssl.ApplicationProtocolNames;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.util.SelfSignedCertificate;
// Required Imports
import io.netty.incubator.codec.http3.Http3ConnectionHandler;
import io.netty.incubator.codec.http3.Http3HeadersFrame;
import io.netty.incubator.codec.quic.QuicSslContext;
import io.netty.incubator.codec.quic.QuicSslContextBuilder;
import io.netty.incubator.codec.http3.Http3ServerConnectionHandler;
import reactor.core.publisher.Mono;
import reactor.netty.incubator.quic.QuicServer;
import io.netty.incubator.codec.quic.QuicServerCodecBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;

class MainServer {

  // public Mono<Void> start() {
	public static void main(String[] args) {
	  SelfSignedCertificate ssc = null;
	  try {
		ssc = new SelfSignedCertificate();
	} catch (CertificateException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
      QuicSslContext context = QuicSslContextBuilder
    		  .forServer(ssc.key(), "", ssc.cert())
    		  	// .forClient()
    		    .trustManager(InsecureTrustManagerFactory.INSTANCE)
    		    .applicationProtocols("h3") // ALPN protocol for HTTP/3
    		    .build();
      
      // QuicServerCodecBuilder
    		  // not sure where this comes from
    	//	  .configure(ssc.certificate(), ssc.privateKey())
          //    .build();
//    SslContext sslContext = SslContextBuilder.forServer(certFile, keyFile)
//      .applicationProtocolConfig(
//        new ApplicationProtocolConfig(
//          ApplicationProtocolConfig.Protocol.ALPN,
//          ApplicationProtocolConfig.SelectorFailureBehavior.NO_ADVERTISE,
//          ApplicationProtocolConfig.SelectedListenerFailureBehavior.ACCEPT
//        )
//      )
//      .build();

    
    QuicServer quicServer = QuicServer.create()
      .secure(context)
      .port(8443)
      .wiretap(true);

    quicServer
      .bind()
      .handle((conn, prop) ->
        conn
          // .addHandlerLast("http3Handler", new Http3ServerConnectionHandler())
          .addHandlerLast("fortuneHandler", new FortuneHeaderFrameHandler())
      ).block();
  }
  
  
  
}
