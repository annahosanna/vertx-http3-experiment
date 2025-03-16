package scratch;

// https://gist.github.com/markscottwright/331bde6028352098883e07b16110732f
import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.Security;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.cms.CMSTypedData;
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;

public class Pkcs7 {

  private static final String KEYSTORE_FILENAME = "...";
  private static final String KEYSTORE_PASSWORD = "...";
  private static final String KEY_ALIAS = "...";
  private static final String KEY_PASSWORD = "...";

  public static void main(String[] args)
    throws OperatorCreationException, IOException, CMSException, KeyStoreException, NoSuchAlgorithmException, CertificateException, UnrecoverableKeyException {
    String data = "Hello";

    // Get keys and provider. If using a token or something, this would be
    // different.
    Security.addProvider(new BouncyCastleProvider());
    KeyStore keystore = KeyStore.getInstance("PKCS12");
    try (var keystoreContents = new FileInputStream(KEYSTORE_FILENAME)) {
      keystore.load(keystoreContents, KEYSTORE_PASSWORD.toCharArray());
    }
    Provider provider = Security.getProvider("BC");
    PrivateKey key = (PrivateKey) keystore.getKey(
      KEY_ALIAS,
      KEY_PASSWORD.toCharArray()
    );
    X509Certificate cert = (X509Certificate) keystore.getCertificate(KEY_ALIAS);

    // Create the signature
    CMSTypedData msg = new CMSProcessableByteArray(data.getBytes());
    CMSSignedDataGenerator signedDataGen = new CMSSignedDataGenerator();
    X509CertificateHolder signCert = new X509CertificateHolder(
      cert.getEncoded()
    );
    ContentSigner signer = new JcaContentSignerBuilder("SHA1withRSA")
      .setProvider(provider)
      .build(key);
    signedDataGen.addSignerInfoGenerator(
      new JcaSignerInfoGeneratorBuilder(
        new JcaDigestCalculatorProviderBuilder().setProvider(provider).build()
      ).build(signer, signCert)
    );

    // add the signing cert to the signature
    Store<X509Certificate> certs = new JcaCertStore(Arrays.asList(cert));
    signedDataGen.addCertificates(certs);

    // false = create detached signature
    CMSSignedData signedData = signedDataGen.generate(msg, false);
    byte[] signatureBytes = signedData.getEncoded();

    org.apache.commons.io.HexDump.dump(signatureBytes, 0, System.out, 0);
  }
}
