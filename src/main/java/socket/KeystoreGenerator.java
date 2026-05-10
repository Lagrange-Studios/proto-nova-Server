package socket;

import java.io.File;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import main.Console;

/**
 * Automatically generates a self-signed keystore if one doesn't exist.
 * Called on server startup to ensure SSL certificates are always available.
 */
public class KeystoreGenerator {
    
    private static String KEYSTORE_PATH;
    private static String KEYSTORE_PASSWORD;
    private static String CERT_ALIAS = "proto-nova-server";
    private static int VALIDITY_DAYS;
    
    private static Console console;
    
    /**
     * Ensures keystore exists. If it doesn't, generates a new self-signed certificate.
     * Called automatically on server startup.
     */
    public static void ensureKeystoreExists(Console console) throws Exception {
        KeystoreGenerator.console = console;
        
        // Load config values (read from memory, not file)
        KEYSTORE_PATH = main.ServerConfig.getInstance().getKeystorePath();
        KEYSTORE_PASSWORD = main.ServerConfig.getInstance().getKeystorePassword();
        VALIDITY_DAYS = main.ServerConfig.getInstance().getKeystoreValidityDays();
        
        File keystoreFile = new File(KEYSTORE_PATH);
        
        if (keystoreFile.exists()) {
            logMessage("✓ Keystore found: " + KEYSTORE_PATH);
            return;
        }
        
        logMessage("⚠ Keystore not found. Generating self-signed certificate...");
        generateKeystore();
        logMessage("✓ Keystore created successfully: " + KEYSTORE_PATH);
    }
    
    /**
     * Generates a new self-signed certificate using keytool.
     */
    private static void generateKeystore() throws Exception {
        ProcessBuilder pb = new ProcessBuilder(
            "keytool",
            "-genkeypair",
            "-alias", CERT_ALIAS,
            "-keyalg", "RSA",
            "-keysize", "2048",
            "-keystore", KEYSTORE_PATH,
            "-storepass", KEYSTORE_PASSWORD,
            "-keypass", KEYSTORE_PASSWORD,
            "-validity", String.valueOf(VALIDITY_DAYS),
            "-dname", "CN=localhost, O=Proto-Nova, L=WorldRoot"
        );
        
        pb.redirectErrorStream(true);
        Process process = pb.start();
        
        // Read output
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        while ((line = reader.readLine()) != null) {
            logMessage(line);
        }
        
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new Exception("Failed to generate keystore. Ensure Java JDK is installed with keytool.");
        }
    }
    
    /**
     * Log message to console if available, otherwise to System.out
     */
    private static void logMessage(String message) {
        if (console != null) {
            console.print(message);
        } else {
            System.out.println(message);
        }
    }
}

