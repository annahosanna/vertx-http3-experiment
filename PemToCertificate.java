package example;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class PemToCertificate {

  public static X509Certificate[] convertPemToX509Certificates(
    String pemContent
  ) throws CertificateException, IOException {
    List<X509Certificate> certificates = new ArrayList<>();
    CertificateFactory cf = CertificateFactory.getInstance("X.509");
    String[] pemCerts = pemContent.split("-----BEGIN CERTIFICATE-----");

    for (String pemCert : pemCerts) {
      if (pemCert.trim().isEmpty()) {
        continue;
      }

      String certContent = pemCert
        .replace("-----END CERTIFICATE-----", "")
        .trim();
      byte[] decodedCertContent = Base64.getMimeDecoder().decode(certContent);

      try (
        ByteArrayInputStream bais = new ByteArrayInputStream(decodedCertContent)
      ) {
        Certificate certificate = cf.generateCertificate(bais);
        certificates.add((X509Certificate) certificate);
      }
    }
    return certificates.toArray(new X509Certificate[0]);
  }

  public static void main(String[] args) {
    String pemContent =
      "-----BEGIN CERTIFICATE-----\n" +
      "MII...\n" + // Example content, replace with actual PEM content
      "-----END CERTIFICATE-----\n" +
      "-----BEGIN CERTIFICATE-----\n" +
      "MII...\n" + // Optional second certificate
      "-----END CERTIFICATE-----\n";

    try {
      X509Certificate[] certs = convertPemToX509Certificates(pemContent);
      System.out.println("Number of certificates: " + certs.length);
      for (X509Certificate cert : certs) {
        System.out.println(
          "Certificate Subject: " + cert.getSubjectX500Principal()
        );
      }
    } catch (CertificateException | IOException e) {
      e.printStackTrace();
    }
  }
}
/*
private PrivateKey loadPrivateKey() throws Exception {
    // Implementation to load private key
    return null;
}

private X509Certificate[] loadCertChain() throws Exception {
    // Implementation to load certificate chain
    return null;
}

X509Certificate.getInstance(FileInputStream)

new File("cert.pem"), new File("key.pem"))

javax.security.cert.X509Certificate
*/
