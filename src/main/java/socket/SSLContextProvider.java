package socket;

import javax.net.ssl.SSLContext;

/**
 * Delegates SSL context creation to EmbeddedSSLProvider.
 * This provider uses keystores embedded in the application resources,
 * eliminating file system dependencies.
 * 
 * @deprecated Use EmbeddedSSLProvider directly for new code.
 */
@Deprecated
public class SSLContextProvider {
    
    /**
     * Creates and returns an SSLContext for server-side SSL connections.
     * Delegates to EmbeddedSSLProvider which loads from embedded resources.
     */
    public static SSLContext getServerSSLContext() throws Exception {
        return EmbeddedSSLProvider.getServerSSLContext();
    }
    
    /**
     * Creates and returns an SSLContext for client-side SSL connections.
     * Delegates to EmbeddedSSLProvider which loads from embedded resources.
     */
    public static SSLContext getClientSSLContext() throws Exception {
        return EmbeddedSSLProvider.getClientSSLContext();
    }
}
