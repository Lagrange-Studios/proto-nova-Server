package security;

import static org.junit.Assert.*;

import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.Date;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class PemCertificateLoaderTest {
  @Rule public TemporaryFolder files = new TemporaryFolder();

  @Test
  public void loadsRsaAndEcKeys() throws Exception {
    for (String algorithm : new String[] {"RSA", "EC"}) {
      KeyPair pair = key(algorithm);
      Path certificate = write(certificate(pair, false));
      Path privateKey = write(pair.getPrivate());
      var loaded = PemCertificateLoader.load(certificate, privateKey);
      assertEquals(
          pair.getPublic(), loaded.keyStore.getCertificate("proto-nova-server").getPublicKey());
      assertNotNull(loaded.keyStore.getKey("proto-nova-server", loaded.password));
    }
  }

  @Test
  public void rejectsMismatchedKey() throws Exception {
    Path certificate = write(certificate(key("RSA"), false));
    Path privateKey = write(key("RSA").getPrivate());
    assertThrows(
        IllegalArgumentException.class, () -> PemCertificateLoader.load(certificate, privateKey));
  }

  @Test
  public void rejectsExpiredCertificate() throws Exception {
    KeyPair pair = key("RSA");
    Path certificate = write(certificate(pair, true));
    Path privateKey = write(pair.getPrivate());
    assertThrows(
        java.security.cert.CertificateExpiredException.class,
        () -> PemCertificateLoader.load(certificate, privateKey));
  }

  @Test
  public void rejectsMissingKey() throws Exception {
    Path certificate = write(certificate(key("RSA"), false));
    assertThrows(
        java.nio.file.NoSuchFileException.class,
        () ->
            PemCertificateLoader.load(
                certificate, files.getRoot().toPath().resolve("missing.pem")));
  }

  private KeyPair key(String algorithm) throws Exception {
    KeyPairGenerator generator =
        KeyPairGenerator.getInstance(
            algorithm, new org.bouncycastle.jce.provider.BouncyCastleProvider());
    generator.initialize(algorithm.equals("EC") ? 256 : 2048);
    return generator.generateKeyPair();
  }

  private X509Certificate certificate(KeyPair pair, boolean expired) throws Exception {
    Instant now = Instant.now();
    X500Name name = new X500Name("CN=play.proto-nova.net");
    var builder =
        new JcaX509v3CertificateBuilder(
            name,
            BigInteger.ONE,
            Date.from(now.minusSeconds(3600)),
            Date.from(now.plusSeconds(expired ? -60 : 3600)),
            name,
            pair.getPublic());
    String algorithm =
        pair.getPrivate().getAlgorithm().equals("EC") ? "SHA256withECDSA" : "SHA256withRSA";
    return new JcaX509CertificateConverter()
        .getCertificate(
            builder.build(new JcaContentSignerBuilder(algorithm).build(pair.getPrivate())));
  }

  private Path write(Object value) throws Exception {
    Path path = files.newFile().toPath();
    try (JcaPEMWriter writer = new JcaPEMWriter(Files.newBufferedWriter(path))) {
      writer.writeObject(value);
    }
    return path;
  }
}
