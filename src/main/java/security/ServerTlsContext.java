package security;

import java.security.SecureRandom;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;

/** Creates TLS contexts for the game and HTTPS listeners. */
public final class ServerTlsContext {
  private ServerTlsContext() {}

  public static SSLContext create() throws Exception {
    main.ServerConfig config = main.ServerConfig.getInstance();
    var certificate =
        java.nio.file.Path.of(
            config.getTlsCertificatePath().isEmpty()
                ? "tls-request/server-https.pem"
                : config.getTlsCertificatePath());
    boolean usePem =
        !config.getTlsCertificatePath().isEmpty() || java.nio.file.Files.exists(certificate);
    var key =
        java.nio.file.Path.of(
            config.getTlsCertificatePath().isEmpty()
                ? "tls-request/server-https-key.pem"
                : config.getTlsPrivateKeyPath());
    if (usePem && config.getTlsCertificatePath().isEmpty()) {
      String bundle = java.nio.file.Files.readString(certificate);
      var match =
          java.util.regex.Pattern.compile(
                  "-----BEGIN (?:RSA )?PRIVATE KEY-----[\\s\\S]*?-----END (?:RSA )?PRIVATE KEY-----")
              .matcher(bundle);
      if (match.find()) {
        // Validate the complete bundle before replacing any existing private key.
        PemCertificateLoader.load(certificate, certificate);
        java.nio.file.Files.writeString(key, match.group() + "\n");
        try {
          java.nio.file.Files.setPosixFilePermissions(
              key, java.nio.file.attribute.PosixFilePermissions.fromString("rw-------"));
        } catch (UnsupportedOperationException ignored) {
        }
      }
    }
    KeystoreManager.LoadedKeystore loaded =
        usePem ? PemCertificateLoader.load(certificate, key) : KeystoreManager.loadOrCreate();
    KeyManagerFactory keyManagers =
        KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
    keyManagers.init(loaded.keyStore, loaded.password);
    SSLContext context = SSLContext.getInstance("TLS");
    var managers = keyManagers.getKeyManagers();
    if (usePem) {
      for (int i = 0; i < managers.length; i++)
        if (managers[i] instanceof javax.net.ssl.X509ExtendedKeyManager manager)
          managers[i] = new ReloadingKeyManager(manager, certificate, key);
      AutomaticCertificateUpdate.start(certificate, key);
    }
    context.init(managers, null, new SecureRandom());
    java.util.Arrays.fill(loaded.password, '\0');
    return context;
  }
}
