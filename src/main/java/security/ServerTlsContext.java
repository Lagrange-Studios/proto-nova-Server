package security;

import java.security.SecureRandom;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;

/** Creates TLS contexts for the game and HTTPS listeners. */
public final class ServerTlsContext {
  private ServerTlsContext() {}

  public static SSLContext create() throws Exception {
    main.ServerConfig config = main.ServerConfig.getInstance();
    KeystoreManager.LoadedKeystore loaded =
        config.getTlsCertificatePath().isEmpty()
            ? KeystoreManager.loadOrCreate()
            : PemCertificateLoader.load(
                java.nio.file.Path.of(config.getTlsCertificatePath()),
                java.nio.file.Path.of(config.getTlsPrivateKeyPath()));
    KeyManagerFactory keyManagers =
        KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
    keyManagers.init(loaded.keyStore, loaded.password);
    SSLContext context = SSLContext.getInstance("TLS");
    var managers = keyManagers.getKeyManagers();
    if (!config.getTlsCertificatePath().isEmpty()) {
      var certificate = java.nio.file.Path.of(config.getTlsCertificatePath());
      var key = java.nio.file.Path.of(config.getTlsPrivateKeyPath());
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
