package security;

import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;

/** Loads CA-issued fullchain.pem and privkey.pem without copying keys into a keystore. */
final class PemCertificateLoader {
  private PemCertificateLoader() {}

  static KeystoreManager.LoadedKeystore load(Path certificatePath, Path keyPath) throws Exception {
    Certificate[] chain;
    try (var input = Files.newInputStream(certificatePath)) {
      chain =
          CertificateFactory.getInstance("X.509")
              .generateCertificates(input)
              .toArray(Certificate[]::new);
    }
    if (chain.length == 0) throw new IllegalArgumentException("TLS certificate chain is empty.");
    for (Certificate certificate : chain) ((X509Certificate) certificate).checkValidity();
    PrivateKey key;
    try (Reader reader = Files.newBufferedReader(keyPath);
        PEMParser parser = new PEMParser(reader)) {
      Object pem = parser.readObject();
      JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
      if (pem instanceof PEMKeyPair pair) key = converter.getPrivateKey(pair.getPrivateKeyInfo());
      else if (pem instanceof PrivateKeyInfo info) key = converter.getPrivateKey(info);
      else
        throw new IllegalArgumentException(
            "TLS private key must be an unencrypted PEM private key.");
    }
    String algorithm =
        switch (key.getAlgorithm()) {
          case "RSA" -> "SHA256withRSA";
          case "EC", "ECDSA" -> "SHA256withECDSA";
          default ->
              throw new IllegalArgumentException(
                  "Unsupported TLS key algorithm: " + key.getAlgorithm());
        };
    Signature proof = Signature.getInstance(algorithm);
    byte[] challenge = new byte[32];
    new java.security.SecureRandom().nextBytes(challenge);
    proof.initSign(key);
    proof.update(challenge);
    byte[] signed = proof.sign();
    proof.initVerify(chain[0].getPublicKey());
    proof.update(challenge);
    if (!proof.verify(signed))
      throw new IllegalArgumentException("TLS certificate and private key do not match.");
    for (int i = 0; i + 1 < chain.length; i++) chain[i].verify(chain[i + 1].getPublicKey());
    char[] password = new char[0];
    KeyStore store = KeyStore.getInstance("PKCS12");
    store.load(null, password);
    store.setKeyEntry("proto-nova-server", key, password, chain);
    return new KeystoreManager.LoadedKeystore(store, password);
  }
}
