package example;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.incubator.codec.http3.DefaultHttp3Headers;
import io.netty.incubator.codec.http3.Http3;
// import io.netty.incubator.codec.http3.Http3ServerChannel;
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

// import org.bouncycastle.x509.X509V3CertificateGenerator;
// import reactor.netty.http.server.QuicServer;

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
