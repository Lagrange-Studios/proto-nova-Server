package security;

import java.net.Socket;
import java.nio.file.*;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import javax.net.ssl.*;

final class ReloadingKeyManager extends X509ExtendedKeyManager {
  private volatile X509ExtendedKeyManager current;
  private final Path certificate, key;
  private java.nio.file.attribute.FileTime stamp;

  ReloadingKeyManager(X509ExtendedKeyManager initial, Path certificate, Path key) throws Exception {
    current = initial;
    this.certificate = certificate;
    this.key = key;
    stamp = Files.getLastModifiedTime(certificate);
  }

  private synchronized void reload() {
    try {
      var next = Files.getLastModifiedTime(certificate);
      if (next.equals(stamp)) return;
      var loaded = PemCertificateLoader.load(certificate, key);
      var f = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
      f.init(loaded.keyStore, loaded.password);
      for (var m : f.getKeyManagers())
        if (m instanceof X509ExtendedKeyManager x) {
          if (!java.util.Arrays.equals(
              current.getCertificateChain("proto-nova-server")[0].getPublicKey().getEncoded(),
              x.getCertificateChain("proto-nova-server")[0].getPublicKey().getEncoded()))
            throw new IllegalStateException("Renewal changed private key.");
          current = x;
          stamp = next;
        }
      java.util.Arrays.fill(loaded.password, '\0');
    } catch (Exception e) {
      System.err.println("Certificate reload failed; retaining previous TLS identity.");
    }
  }

  public String[] getClientAliases(String k, Principal[] i) {
    return current.getClientAliases(k, i);
  }

  public String chooseClientAlias(String[] k, Principal[] i, Socket s) {
    return current.chooseClientAlias(k, i, s);
  }

  public String[] getServerAliases(String k, Principal[] i) {
    reload();
    return current.getServerAliases(k, i);
  }

  public String chooseServerAlias(String k, Principal[] i, Socket s) {
    reload();
    return current.chooseServerAlias(k, i, s);
  }

  public String chooseEngineServerAlias(String k, Principal[] i, SSLEngine e) {
    reload();
    return current.chooseEngineServerAlias(k, i, e);
  }

  public String chooseEngineClientAlias(String[] k, Principal[] i, SSLEngine e) {
    return current.chooseEngineClientAlias(k, i, e);
  }

  public X509Certificate[] getCertificateChain(String a) {
    return current.getCertificateChain(a);
  }

  public PrivateKey getPrivateKey(String a) {
    return current.getPrivateKey(a);
  }
}
