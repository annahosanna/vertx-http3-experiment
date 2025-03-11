package example;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.incubator.codec.http3.DefaultHttp3Headers;
import io.netty.incubator.codec.http3.Http3;
import io.netty.incubator.codec.http3.Http3ServerChannel;
import io.netty.incubator.codec.http3.Http3ServerConnectionHandler;
import io.netty.util.CharsetUtil;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import javax.security.auth.x500.X500Principal;
import org.bouncycastle.x509.X509V3CertificateGenerator;
import reactor.netty.http.server.QuicServer;

public class FortuneServer {

  public static void main(String[] args) {
    CertificateProvider certProvider = new CertificateProvider();
    FortunesHandler fortunesHandler = new FortunesHandler();

    QuicServer.create()
      .port(8443)
      .secure(spec -> spec.certificate(certProvider.getCertificate()))
      .handle((request, response) -> {
        if (request.path().equals("/fortunes")) {
          return fortunesHandler.handle(request, response);
        }
        return response.sendNotFound();
      })
      .bindNow()
      .onDispose()
      .block();
  }
}
