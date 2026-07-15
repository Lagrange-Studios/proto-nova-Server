package security;

import java.security.SecureRandom;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;

/** Creates the TLS context used only by the game server socket. */
public final class ServerTlsContext {
    private ServerTlsContext() {}

    public static SSLContext create() throws Exception {
        KeystoreManager.LoadedKeystore loaded = KeystoreManager.loadOrCreate();
        KeyManagerFactory keyManagers = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagers.init(loaded.keyStore, loaded.password);
        SSLContext context = SSLContext.getInstance("TLS");
        context.init(keyManagers.getKeyManagers(), null, new SecureRandom());
        java.util.Arrays.fill(loaded.password, '\0');
        return context;
    }
}
