package socket;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.Enumeration;

/**
 * Utility class to extract the server certificate from keystore and save it as a .cer file.
 * This allows clients on remote machines to download the certificate.
 */
public class CertificateExtractor {
    
    private static String KEYSTORE_PATH;
    private static String KEYSTORE_PASSWORD;
    private static final String KEYSTORE_ALGORITHM = "PKCS12";
    private static final String CERT_ALIAS = "proto-nova";
    private static final String CERT_FILE_PATH = "proto-nova-cert.cer";
    
    /**
     * Extracts the server certificate from the keystore and saves it as a .cer file.
     * This certificate can be served to clients for download.
     */
    public static void ensureCertificateExists() throws Exception {
        // Load config values
        KEYSTORE_PATH = main.ServerConfig.getInstance().getKeystorePath();
        KEYSTORE_PASSWORD = main.ServerConfig.getInstance().getKeystorePassword();
        
        File certFile = new File(CERT_FILE_PATH);
        if (certFile.exists()) {
            return; // Certificate already exported
        }
        
        try {
            // Load the keystore
            KeyStore keyStore = KeyStore.getInstance(KEYSTORE_ALGORITHM);
            try (FileInputStream fis = new FileInputStream(KEYSTORE_PATH)) {
                keyStore.load(fis, KEYSTORE_PASSWORD.toCharArray());
            }
            
            // Find and extract the certificate
            X509Certificate certificate = null;
            
            // First, try to get the certificate with the known alias
            if (keyStore.containsAlias(CERT_ALIAS)) {
                certificate = (X509Certificate) keyStore.getCertificate(CERT_ALIAS);
            } else {
                // If not found, get the first certificate
                Enumeration<String> aliases = keyStore.aliases();
                if (aliases.hasMoreElements()) {
                    String alias = aliases.nextElement();
                    certificate = (X509Certificate) keyStore.getCertificate(alias);
                }
            }
            
            if (certificate == null) {
                throw new Exception("No certificate found in keystore");
            }
            
            // Export certificate to file
            byte[] encodedCert = certificate.getEncoded();
            try (FileOutputStream fos = new FileOutputStream(CERT_FILE_PATH)) {
                fos.write(encodedCert);
            }
            
            System.out.println("✓ Certificate exported to: " + CERT_FILE_PATH);
        } catch (Exception e) {
            System.err.println("Failed to extract certificate: " + e.getMessage());
            throw e;
        }
    }
    
    /**
     * Returns the path to the exported certificate file.
     */
    public static String getCertificateFilePath() {
        return CERT_FILE_PATH;
    }
}
