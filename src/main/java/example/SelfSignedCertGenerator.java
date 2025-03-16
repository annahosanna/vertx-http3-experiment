package example;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.math.BigInteger;
import java.security.*;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.MGF1ParameterSpec;
import java.util.Collection;
import java.util.Date;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import oracle.security.crypto.cert.PKCS7;
import oracle.security.crypto.cert.X509;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cms.CMSAlgorithm;
import org.bouncycastle.cms.CMSEnvelopedData;
import org.bouncycastle.cms.CMSEnvelopedDataGenerator;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.KeyTransRecipientInformation;
import org.bouncycastle.cms.RecipientInformation;
import org.bouncycastle.cms.bc.BcCMSContentEncryptorBuilder;
import org.bouncycastle.cms.jcajce.JceKeyTransEnvelopedRecipient;
import org.bouncycastle.cms.jcajce.JceKeyTransRecipientInfoGenerator;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PKCS8Generator;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.openssl.jcajce.JcaPKCS8Generator;
import org.bouncycastle.openssl.jcajce.JceOpenSSLPKCS8EncryptorBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.OutputEncryptor;
import org.bouncycastle.operator.jcajce.JcaAlgorithmParametersConverter;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.util.encoders.Hex;
import org.bouncycastle.util.io.pem.PemGenerationException;
import org.bouncycastle.util.io.pem.PemObject;

//

public class SelfSignedCertGenerator {

  private KeyPair keyPair;
  private X509Certificate certificate;

  public SelfSignedCertGenerator() throws CertificateException {
    try {
      generateKeyPair();
      generateCertificate();
    } catch (Exception e) {
      throw new CertificateException(e);
    }
  }

  private void generateKeyPair() throws CertificateException {
    try {
      KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
      keyPairGenerator.initialize(2048);
      this.keyPair = keyPairGenerator.generateKeyPair();
    } catch (Exception e) {
      throw new CertificateException(e);
    }
  }

  private void generateCertificate() throws CertificateException {
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

      // returns an x509 certificate object
      this.certificate = new JcaX509CertificateConverter()
        .getCertificate(certHolder);
      // So it can be written to a pkcs8 pem private key not encrypted file
      // and x509 pem
      // This should be pem format
      //  byte[] cert = this.certificate.getEncoded();

    } catch (Exception e) {
      throw new CertificateException(e);
    }
  }

  // PKCS 8 format, unencrypted
  public void getPrivateKey() {
    // return this.keyPair.getPrivate().getEncoded();

    try {
      JcaPKCS8Generator jcaPKCS8Generator = new JcaPKCS8Generator(
        keyPair.getPrivate(),
        null
      );
      PemObject pemObject = jcaPKCS8Generator.generate();
      StringWriter pemStringWriter = new StringWriter();
      try (JcaPEMWriter jcaPEMWriter = new JcaPEMWriter(pemStringWriter)) {
        jcaPEMWriter.writeObject(pemObject);
      } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
      String pkcs8KeyString = pemStringWriter.toString();
      // Convert this to a try with resources
      FileOutputStream pemFileOutputStream = new FileOutputStream(
        "privatekey-unencrypted.pem"
      );
      pemFileOutputStream.write(pkcs8KeyString.getBytes());
      pemFileOutputStream.flush();
      pemFileOutputStream.close();
    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  /*
  // https://developer.dbp.thalescloud.io/docs/tsh-token-push-and-control/e1c34e566ad71-encrypted-card-data-in-pkcs-7-format
  private static byte[] encryptPKCS7(byte[] plainData, PublicKey pubKey)
    throws Exception {
    CMSEnvelopedDataGenerator gen = new CMSEnvelopedDataGenerator();

    JcaAlgorithmParametersConverter paramsConverter =
      new JcaAlgorithmParametersConverter();
    OAEPParameterSpec oaepParamSpec = new OAEPParameterSpec(
      "SHA-256",
      "MGF1",
      MGF1ParameterSpec.SHA256,
      PSource.PSpecified.DEFAULT
    );
    AlgorithmIdentifier algoId = paramsConverter.getAlgorithmIdentifier(
      PKCSObjectIdentifiers.id_RSAES_OAEP,
      oaepParamSpec
    );

    JceKeyTransRecipientInfoGenerator recipInfo =
      new JceKeyTransRecipientInfoGenerator(
        KEY_IDENTIFIER.getBytes(),
        algoId,
        pubKey
      ).setProvider(bcProvider);

    gen.addRecipientInfoGenerator(recipInfo);

    CMSProcessableByteArray data = new CMSProcessableByteArray(plainData);
    BcCMSContentEncryptorBuilder builder = new BcCMSContentEncryptorBuilder(
      CMSAlgorithm.AES256_CBC
    );

    CMSEnvelopedData enveloped = gen.generate(data, builder.build());

    return enveloped.getEncoded();
  }
*/
  // Der format needs to be pem
  // Or PKCS 7 for certificate chain (pfx includes public and private in single file)
  public void getCertificate() {
    try {
      byte[] certAsDER = certificate.getEncoded();
      oracle.security.crypto.cert.X509 x509OracleObject =
        new oracle.security.crypto.cert.X509(certAsDER);
      oracle.security.crypto.cert.PKCS7 pkcs7OracleObject =
        new oracle.security.crypto.cert.PKCS7(x509OracleObject);
      byte[] pkcs7DER = pkcs7OracleObject.getEncoded();
      FileOutputStream pemPKCS7FileOutputStream = new FileOutputStream(
        "certificate-chain.pem"
      );
      pemPKCS7FileOutputStream.write(pkcs7DER);
      pemPKCS7FileOutputStream.flush();
      pemPKCS7FileOutputStream.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
