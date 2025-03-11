package example;

import java.math.BigInteger;
import java.security.*;
import java.security.cert.X509Certificate;
import java.util.Date;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;

public class SelfSignedCertGenerator {

  private KeyPair keyPair;
  private X509Certificate certificate;

  public SelfSignedCertGenerator() {
    generateKeyPair();
    generateCertificate();
  }

  private void generateKeyPair() {
	  try {
    KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
    keyPairGenerator.initialize(2048);
    this.keyPair = keyPairGenerator.generateKeyPair();
	  } catch (Exception e) {
		  
	  }
  }

  private void generateCertificate() {
	  try {
    X500Name issuerName = new X500Name("CN=localhost");
    X500Name subjectName = new X500Name("CN=localhost");

    BigInteger serialNumber = BigInteger.valueOf(System.currentTimeMillis());
    Date startDate = new Date(System.currentTimeMillis());
    Date endDate = new Date(
      System.currentTimeMillis() + 365 * 24 * 60 * 60 * 1000L
    );

    X509v3CertificateBuilder certBuilder = new X509v3CertificateBuilder(
      issuerName,
      serialNumber,
      startDate,
      endDate,
      subjectName,
      SubjectPublicKeyInfo.getInstance(this.keyPair.getPublic().getEncoded())
    );

    // Add Subject Alternative Name
    GeneralName altName = new GeneralName(GeneralName.dNSName, "localhost");
    GeneralNames subjectAltName = new GeneralNames(altName);
    certBuilder.addExtension(
      Extension.subjectAlternativeName,
      false,
      subjectAltName
    );

    ContentSigner signer = new JcaContentSignerBuilder("SHA256WithRSA").build(
      this.keyPair.getPrivate()
    );
    X509CertificateHolder certHolder = certBuilder.build(signer);

    this.certificate = new JcaX509CertificateConverter()
      .getCertificate(certHolder);
	  } catch (Exception e) {
		  
	  }
  }

  public PrivateKey getPrivateKey() {
    return this.keyPair.getPrivate();
  }

  public X509Certificate getCertificate() {
    return certificate;
  }
}
