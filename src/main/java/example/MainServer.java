package example;

// import io.netty.incubator.codec.quic.QuicServerCodecBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.ssl.util.SimpleTrustManagerFactory;
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
import java.net.InetSocketAddress;
import java.security.cert.CertificateException;
import reactor.core.publisher.Mono;
// import io.netty.incubator.codec.quic.QuicTokenHandler;
// import io.netty.incubator.codec.http3.*;
// import io.netty.incubator.codec.http3.Http3ServerConnectionHandler;
// import io.netty.incubator.codec.http3.Http3ConnectionHandler;
// import io.netty.incubator.codec.http3.Http3ServerConnectionHandler;
// import reactor.core.publisher.Mono;
import reactor.netty.incubator.quic.QuicServer;
import javax.net.ssl.TrustManagerFactory;
import java.io.File;

class MainServer {

  // public Mono<Void> start() {
  public static void main(String[] args) {
	
	  // See the bottom of this class for notes
	  
	  // This will not work needs to use the same cert as whatever is on port 8443/tcp
    /*
	  SelfSignedCertGenerator ssc = null;
    try {
      ssc = new SelfSignedCertGenerator();
    } catch (CertificateException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    */
    // Fix this
	// TrustManagerFactory trustManagerFactory = SimpleTrustManagerFactory.getInstance(SimpleTrustManagerFactory.getDefaultAlgorithm());
    // Figure out how to use pem from files
    // QuicSslContextBuilder.forServer(java.security.PrivateKey key, java.lang.String keyPassword, java.security.cert.X509Certificate[] certChain);
    // or use the trustmanager and keymanager methods to specify a file
    // x509 cert file in pem format and pkcs8 private key pem format
	  // Public: Pem encoded x509 certificate chain
	  // Private PKCS* pem
	  File keyFile = new File("key-file.pem");
	  File certsFile = new File("certs.pem");
    QuicSslContext context = QuicSslContextBuilder.forServer(keyFile, null, certsFile)

/*
    		ssc.getPrivateKey(),
      null,
      ssc.getCertificate()
    )
    */
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

    // bind takes a bind(mono ->)
    // bindNow() not

    // complaining that bind and bindNow are null
    try {
      quicServer
        .bindNow()
        // This handler doesn't seem like a stream handler
        // .addHandlerLast("fortuneHandler", new FortuneHeaderFrameHandler())
        .bind()
        .onDispose()
        .block();
    } catch (Exception e) {
      // InterruptedException
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    //      .handle((conn, prop) ->
    //        conn
    // .addHandlerLast("http3Handler", new Http3ServerConnectionHandler())
    //          .addHandlerLast("fortuneHandler", new FortuneHeaderFrameHandler())
    //      ).block();
  }
}

/*
public static io.netty.incubator.codec.quic.QuicSslContextBuilder forServer(java.security.PrivateKey key, java.lang.String keyPassword, java.security.cert.X509Certificate[] certChain);

io.netty.incubator.codec.quic.QuicSslContextBuilder.keyManager
javax.net.ssl.KeyManagerFactory keyManagerFactory,
java.lang.String password
      
io.netty.incubator.codec.quic.QuicSslContextBuilder.build()
      
public static io.netty.incubator.codec.quic.QuicSslContext buildForServerWithSni(io.netty.util.Mapping mapping);
io.netty.incubator.codec.quic.QuicSslContextBuilder.SNI_KEYMANAGER : javax.net.ssl.X509ExtendedKeyManager
io.netty.util.Mapping<? super java.lang.String,? extends io.netty.incubator.codec.quic.QuicSslContext>

io.netty.incubator.codec.quic.QuicSslContextBuilder trustManager(java.io.File trustCertCollectionFile);
io.netty.incubator.codec.quic.QuicSslContextBuilder.trustManager(java.security.cert.X509Certificate[]) : 

io.netty.incubator.codec.quic.QuicSslContextBuilder keyManager(java.io.File keyFile, java.lang.String keyPassword, java.io.File keyCertChainFile);

io.netty.incubator.codec.quic.QuicSslContextBuilder keyManager(java.security.PrivateKey key, java.lang.String keyPassword, java.security.cert.X509Certificate[]);

io.netty.incubator.codec.quic.QuicSslContextBuilder keyManager(javax.net.ssl.KeyManagerFactory keyManagerFactory, java.lang.String keyPassword);

io.netty.incubator.codec.quic.QuicSslContextBuilder keyManager(javax.net.ssl.KeyManager keyManager, java.lang.String password);
*/
