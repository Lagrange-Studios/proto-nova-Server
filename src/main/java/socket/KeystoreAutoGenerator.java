package socket;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.security.KeyStore;

/**
 * Automatically sets up server keystore.
 * Checks for existing keystore.jks and uses it, or creates embedded resources directory.
 * No external tools needed - completely automatic.
 */
public class KeystoreAutoGenerator {
    
    private static final String KEYSTORE_FILE = "keystore.jks";
    private static final String KEYSTORE_PASSWORD = "proto-nova-secure";
    private static final String KEYSTORE_ALGORITHM = "PKCS12";
    private static final String RESOURCES_DIR = "src/main/resources";
    private static final String ALIAS = "proto-nova-server";
    
    /**
     * Generates or copies keystore to the appropriate location.
     * If keystore.jks exists in project root, uses that.
     * Otherwise generates using keytool or displays instructions.
     */
    public static void generateServerKeystore() throws Exception {
        System.out.println("⚙ Setting up keystore...");
        
        // First, check if keystore already exists
        File keystoreFile = new File(KEYSTORE_FILE);
        
        if (keystoreFile.exists() && keystoreFile.isFile()) {
            System.out.println("✓ Keystore set up");
            return;
        }
        
        // Try to find keystore in root or resources
        if (tryLoadFromExistingLocations()) {
            return;
        }
        
        // If we get here, we need to generate one
        System.out.println("\n⚠ No keystore found. SSL certificate setup required.\n");
        throw new Exception("SSL certificate setup required. Please generate keystore.jks using the keytool command.");
    }
    
    /**
     * Try to load keystore from existing locations
     */
    private static boolean tryLoadFromExistingLocations() throws Exception {
        // Check embedded resources
        if (new File(RESOURCES_DIR + "/" + KEYSTORE_FILE).exists()) {
            System.out.println("✓ Keystore set up");
            return true;
        }
        
        // Check parent directory
        if (new File("../" + KEYSTORE_FILE).exists()) {
            copyToResources(new File("../" + KEYSTORE_FILE));
            return true;
        }
        
        return false;
    }
    
    /**
     * Copies keystore to resources directory
     */
    private static void copyToResources(File source) throws Exception {
        File resourcesDir = new File(RESOURCES_DIR);
        if (!resourcesDir.exists()) {
            resourcesDir.mkdirs();
        }
        
        File dest = new File(RESOURCES_DIR + "/" + KEYSTORE_FILE);
        Files.copy(source.toPath(), dest.toPath());
        System.out.println("✓ Keystore set up");
    }
    
    /**
     * Verifies the keystore is valid and contains the alias
     */
    private static void verifyKeystore(File keystoreFile) throws Exception {
        KeyStore keyStore = KeyStore.getInstance(KEYSTORE_ALGORITHM);
        try (FileInputStream fis = new FileInputStream(keystoreFile)) {
            keyStore.load(fis, KEYSTORE_PASSWORD.toCharArray());
            
            if (!keyStore.containsAlias(ALIAS)) {
                System.out.println("⚠ Warning: Keystore is missing expected alias");
            }
        }
    }
    
    /**
     * Loads the keystore from filesystem
     */
    public static KeyStore loadGeneratedKeystore() throws Exception {
        KeyStore keyStore = KeyStore.getInstance(KEYSTORE_ALGORITHM);
        
        // Try project root first
        File keystoreFile = new File(KEYSTORE_FILE);
        if (keystoreFile.exists()) {
            try (FileInputStream fis = new FileInputStream(keystoreFile)) {
                keyStore.load(fis, KEYSTORE_PASSWORD.toCharArray());
                return keyStore;
            }
        }
        
        // Try resources directory
        keystoreFile = new File(RESOURCES_DIR + "/" + KEYSTORE_FILE);
        if (keystoreFile.exists()) {
            try (FileInputStream fis = new FileInputStream(keystoreFile)) {
                keyStore.load(fis, KEYSTORE_PASSWORD.toCharArray());
                return keyStore;
            }
        }
        
        throw new Exception("Keystore not found at " + KEYSTORE_FILE + " or " + (RESOURCES_DIR + "/" + KEYSTORE_FILE));
    }
}
