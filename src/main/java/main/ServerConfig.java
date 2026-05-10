package main;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class ServerConfig {
    
    private static final String CONFIG_FILE = "proto-nova.properties";
    private static ServerConfig instance;
    private Properties properties;
    private Console console;
    
    private int gameSocketPort;
    private int statusHttpPort;
    private int certificateServerPort;
    private int ticksPerSecond;
    private int threadPoolSize;
    private int processorLimit;
    private int ramLimit;
    private int workerThreadLimit;
    private String keystorePath;
    private String keystorePassword;
    private int keystoreValidityDays;
    
    private ServerConfig(Console console) throws IOException {
        this.console = console;
        this.properties = new Properties();
        loadOrCreateConfig();
        parseProperties();
    }
    
    // Initialize ServerConfig once at server startup
    public static void initialize(Console console) throws IOException {
        if (instance == null) {
            instance = new ServerConfig(console);
        }
    }
    
    // Get singleton instance
    public static ServerConfig getInstance() {
        if (instance == null) {
            throw new RuntimeException("ServerConfig not initialized! Call ServerConfig.initialize(console) at server startup.");
        }
        return instance;
    }
    
    // Load config file or create default if it doesn't exist
    private void loadOrCreateConfig() throws IOException {
        File configFile = new File(CONFIG_FILE);
        
        if (!configFile.exists()) {
            console.print("✓ Creating default proto-nova.properties file...");
            createDefaultConfig();
        } else {
            console.print("✓ Loading proto-nova.properties...");
        }
        
        try (FileInputStream fis = new FileInputStream(CONFIG_FILE)) {
            properties.load(fis);
        }
        
        console.print("✓ Configuration loaded successfully");
    }
    
    // Create default configuration file with all settings
    private void createDefaultConfig() throws IOException {
        Properties defaultProps = new Properties();
        
        defaultProps.setProperty("game.socket.port", "7675");
        defaultProps.setProperty("http.status.port", "7674");
        defaultProps.setProperty("http.certificate.port", "7673");
        defaultProps.setProperty("server.tps", "60");
        defaultProps.setProperty("server.thread.pool.size", "50");
        defaultProps.setProperty("server.processor.limit", "0");
        defaultProps.setProperty("server.ram.limit", "0");
        defaultProps.setProperty("server.worker.thread.limit", "32");
        defaultProps.setProperty("keystore.path", "keystore.jks");
        defaultProps.setProperty("keystore.password", "proto-nova-secure");
        defaultProps.setProperty("keystore.validity.days", "365");
        
        try (FileOutputStream fos = new FileOutputStream(CONFIG_FILE)) {
            defaultProps.store(fos, "Proto Nova Server Configuration\n" +
                    "Last generated: " + new java.util.Date() + "\n" +
                    "Edit this file to change server settings. Changes require server restart.");
        }
    }
    
    // Parse all properties from file into memory variables
    private void parseProperties() {
        this.gameSocketPort = getIntProperty("game.socket.port", 7675);
        this.statusHttpPort = getIntProperty("http.status.port", 7674);
        this.certificateServerPort = getIntProperty("http.certificate.port", 7673);
        this.ticksPerSecond = getIntProperty("server.tps", 60);
        this.threadPoolSize = getIntProperty("server.thread.pool.size", 50);
        this.processorLimit = getIntProperty("server.processor.limit", 0);
        this.ramLimit = getIntProperty("server.ram.limit", 0);
        this.workerThreadLimit = getIntProperty("server.worker.thread.limit", 32);
        this.keystorePath = getStringProperty("keystore.path", "keystore.jks");
        this.keystorePassword = getStringProperty("keystore.password", "proto-nova-secure");
        this.keystoreValidityDays = getIntProperty("keystore.validity.days", 365);
        
        console.print("═══ Server Configuration ═══");
        console.print("Game Socket Port: " + gameSocketPort);
        console.print("Status HTTP Port: " + statusHttpPort);
        console.print("Certificate Server Port: " + certificateServerPort);
        console.print("Ticks Per Second: " + ticksPerSecond);
        console.print("Thread Pool Size: " + threadPoolSize);
        console.print("Processor Limit: " + (processorLimit == 0 ? "Unlimited" : processorLimit + " cores"));
        console.print("RAM Limit: " + (ramLimit == 0 ? "Unlimited" : ramLimit + " MB"));
        console.print("Worker Thread Limit: " + workerThreadLimit);
        console.print("Keystore: " + keystorePath);
        console.print("════════════════════════════");
    }
    
    // Get integer property with default fallback
    private int getIntProperty(String key, int defaultValue) {
        try {
            String value = properties.getProperty(key, String.valueOf(defaultValue));
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            console.print("⚠ Invalid integer for property '" + key + "', using default: " + defaultValue);
            return defaultValue;
        }
    }
    
    // Get string property with default fallback
    private String getStringProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }
    
    // Port for SSL/TLS encrypted game connections
    public int getGameSocketPort() { return gameSocketPort; }
    
    // Port for HTTP status and token endpoints
    public int getStatusHttpPort() { return statusHttpPort; }
    
    // Port for certificate download endpoint
    public int getCertificateServerPort() { return certificateServerPort; }
    
    // Game simulation speed in ticks per second
    public int getTicksPerSecond() { return ticksPerSecond; }
    
    // Maximum concurrent player connections
    public int getThreadPoolSize() { return threadPoolSize; }
    
    // Maximum number of processor cores the server can use (0 = unlimited)
    public int getProcessorLimit() { return processorLimit; }
    
    // Maximum RAM in MB the server is allowed to use (0 = unlimited)
    public int getRamLimit() { return ramLimit; }
    
    // Maximum number of worker threads for background tasks (0 = unlimited)
    public int getWorkerThreadLimit() { return workerThreadLimit; }
    
    // Path to SSL keystore file
    public String getKeystorePath() { return keystorePath; }
    
    // Password for SSL keystore
    public String getKeystorePassword() { return keystorePassword; }
    
    // Validity period for self-signed certificates in days
    public int getKeystoreValidityDays() { return keystoreValidityDays; }
}
