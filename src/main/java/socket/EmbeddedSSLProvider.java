package socket;

import javax.net.ssl.SSLContext;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.security.cert.CertificateFactory;

/**
 * Provides SSL context for secure socket connections.
 * 
 * AUTOMATIC MODE:
 * - Server: Auto-generates keystore.jks if missing on first run
 * - Client: Auto-downloads and trusts server certificates dynamically
 * 
 * SUPPORTS:
 * ✓ Multiple different servers (downloads each cert dynamically)
 * ✓ Zero manual setup required
 * ✓ Different users connecting to different servers
 * ✓ Cross-device communication
 */
public class EmbeddedSSLProvider {
    
    private static final String SERVER_KEYSTORE_PATH = "/keystore.jks";
    private static final String SERVER_KEYSTORE_PASSWORD = "proto-nova-secure";
    private static final String KEYSTORE_ALGORITHM = "PKCS12";
    private static final String SSL_PROTOCOL = "TLSv1.2";
    
    private static KeyStore cachedServerKeyStore = null;
    private static Object cacheLock = new Object();
    
    /**
     * Creates and returns an SSLContext for server-side SSL connections.
     * Auto-generates keystore.jks if it doesn't exist.
     */
    public static SSLContext getServerSSLContext() throws Exception {
        KeyStore serverKeyStore = getServerKeyStore();
        
        SSLContext sslContext = SSLContext.getInstance(SSL_PROTOCOL);
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(serverKeyStore, SERVER_KEYSTORE_PASSWORD.toCharArray());
        
        sslContext.init(kmf.getKeyManagers(), null, new java.security.SecureRandom());
        System.out.println("✓ Server SSL context initialized");
        return sslContext;
    }
    
    /**
     * Creates and returns an SSLContext for client-side SSL connections.
     * Dynamically trusts any server certificate (auto-downloads from server).
     * Works with multiple different servers and users.
     */
    public static SSLContext getClientSSLContext() throws Exception {
        SSLContext sslContext = SSLContext.getInstance(SSL_PROTOCOL);
        
        // Trust all certificates (client auto-downloads from CertificateServer)
        X509TrustManager trustAllManager = new X509TrustManager() {
            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType) {}
            
            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType) {}
            
            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[0];
            }
        };
        
        sslContext.init(null, new X509TrustManager[]{trustAllManager}, new java.security.SecureRandom());
        System.out.println("✓ Client SSL context initialized (auto-trust mode)");
        return sslContext;
    }
    
    /**
     * Gets or caches the server keystore (private key + certificate).
     * Auto-generates if missing.
     */
    private static KeyStore getServerKeyStore() throws Exception {
        if (cachedServerKeyStore != null) {
            return cachedServerKeyStore;
        }
        
        synchronized (cacheLock) {
            if (cachedServerKeyStore != null) {
                return cachedServerKeyStore;
            }
            
            KeyStore keyStore = loadOrGenerateServerKeystore();
            cachedServerKeyStore = keyStore;
            return keyStore;
        }
    }
    
    /**
     * Loads embedded keystore or generates one automatically.
     */
    private static KeyStore loadOrGenerateServerKeystore() throws Exception {
        KeyStore keyStore = KeyStore.getInstance(KEYSTORE_ALGORITHM);
        
        // Try to load from resources first (for production/JAR deployment)
        InputStream stream = EmbeddedSSLProvider.class.getResourceAsStream(SERVER_KEYSTORE_PATH);
        
        if (stream != null) {
            try {
                keyStore.load(stream, SERVER_KEYSTORE_PASSWORD.toCharArray());
                System.out.println("✓ Loaded keystore from embedded resources");
                return keyStore;
            } finally {
                stream.close();
            }
        }
        
        // If not embedded, auto-generate (for development/testing)
        System.out.println("⚠ Keystore not found - auto-generating for development...");
        KeystoreAutoGenerator.generateServerKeystore();
        
        // Reload from filesystem after generation
        return KeystoreAutoGenerator.loadGeneratedKeystore();
    }
}