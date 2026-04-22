package socket;

import javax.net.ssl.SSLContext;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.security.KeyStore;

/**
 * Provides SSL context for secure socket connections.
 * The keystore file should be placed in the server root directory.
 */
public class SSLContextProvider {
    
    private static final String KEYSTORE_PATH = "keystore.jks";
    private static final String KEYSTORE_PASSWORD = "proto-nova-secure"; // Change this to a secure password
    private static final String KEYSTORE_ALGORITHM = "PKCS12";
    private static final String SSL_PROTOCOL = "TLSv1.2";
    
    /**
     * Creates and returns an SSLContext for server-side SSL connections.
     * Requires a keystore.jks file to be present.
     */
    public static SSLContext getServerSSLContext() throws Exception {
        // Load the keystore
        KeyStore keyStore = KeyStore.getInstance(KEYSTORE_ALGORITHM);
        try (FileInputStream fis = new FileInputStream(KEYSTORE_PATH)) {
            keyStore.load(fis, KEYSTORE_PASSWORD.toCharArray());
        }
        
        // Initialize KeyManagerFactory with the keystore
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keyStore, KEYSTORE_PASSWORD.toCharArray());
        
        // Create and initialize SSLContext
        SSLContext sslContext = SSLContext.getInstance(SSL_PROTOCOL);
        sslContext.init(kmf.getKeyManagers(), null, new java.security.SecureRandom());
        
        return sslContext;
    }
    
    /**
     * Creates and returns an SSLContext for client-side SSL connections.
     * For now, this uses the same keystore. In production, you may want to use
     * a truststore instead.
     */
    public static SSLContext getClientSSLContext() throws Exception {
        // Load the keystore (in production, use a truststore)
        KeyStore keyStore = KeyStore.getInstance(KEYSTORE_ALGORITHM);
        try (FileInputStream fis = new FileInputStream(KEYSTORE_PATH)) {
            keyStore.load(fis, KEYSTORE_PASSWORD.toCharArray());
        }
        
        // Initialize TrustManagerFactory with the keystore
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(keyStore);
        
        // Create and initialize SSLContext
        SSLContext sslContext = SSLContext.getInstance(SSL_PROTOCOL);
        sslContext.init(null, tmf.getTrustManagers(), new java.security.SecureRandom());
        
        return sslContext;
    }
}
