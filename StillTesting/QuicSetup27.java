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

class FortunesHandler {

  private final ObjectMapper mapper = new ObjectMapper();
  private final List<String> fortunes = Arrays.asList(
    "You will find happiness in unexpected places",
    "A journey of a thousand miles begins with a single step",
    "Good fortune will be yours"
  );

  public reactor.netty.http.server.HttpServerResponse handle(
    reactor.netty.http.server.HttpServerRequest request,
    reactor.netty.http.server.HttpServerResponse response
  ) {
    try {
      String json = mapper.writeValueAsString(fortunes);
      ByteBuf buf = Unpooled.copiedBuffer(json, CharsetUtil.UTF_8);
      return response
        .header("content-type", "application/json")
        .sendByteArray(buf.array());
    } catch (Exception e) {
      return response
        .status(500)
        .sendString(
          reactor.core.publisher.Mono.just("Error generating fortunes")
        );
    }
  }
}

class CertificateProvider {

  private X509Certificate certificate;
  private KeyPair keyPair;

  public CertificateProvider() {
    try {
      KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
      keyGen.initialize(2048);
      keyPair = keyGen.generateKeyPair();

      X509V3CertificateGenerator certGen = new X509V3CertificateGenerator();
      X500Principal dnName = new X500Principal("CN=localhost");

      certGen.setSerialNumber(BigInteger.valueOf(System.currentTimeMillis()));
      certGen.setSubjectDN(dnName);
      certGen.setIssuerDN(dnName);
      certGen.setNotBefore(
        new Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000)
      );
      certGen.setNotAfter(
        new Date(System.currentTimeMillis() + 365 * 24 * 60 * 60 * 1000L)
      );
      certGen.setPublicKey(keyPair.getPublic());
      certGen.setSignatureAlgorithm("SHA256WithRSAEncryption");

      certificate = certGen.generate(keyPair.getPrivate());
    } catch (Exception e) {
      throw new RuntimeException("Failed to create self-signed certificate", e);
    }
  }

  public Certificate getCertificate() {
    return certificate;
  }

  public KeyPair getKeyPair() {
    return keyPair;
  }
}
