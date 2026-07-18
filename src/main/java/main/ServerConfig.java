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
    private int gameSocketIdleTimeoutSeconds;
    private int gameSocketOutboundQueueSize;
    private boolean statusHttpEnabled;
    private int statusHttpPort;
    private String statusHttpBindAddress;
    private int statusHttpWorkerThreads;
    private int statusHttpQueueSize;
    private int ticksPerSecond;
    private int threadPoolSize;
    private int processorLimit;
    private int ramLimit;
    private int workerThreadLimit;
    private String keystorePath;
    private String legacyKeystorePassword;
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
            createDefaultConfig();
        }
        
        try (FileInputStream fis = new FileInputStream(CONFIG_FILE)) {
            properties.load(fis);
        }
        
    }
    
    // Create default configuration file with all settings
    private void createDefaultConfig() throws IOException {
        Properties defaultProps = new Properties();
        
        defaultProps.setProperty("game.socket.port", "7675");
        defaultProps.setProperty("game.socket.idle.timeout.seconds", "300");
        defaultProps.setProperty("game.socket.outbound.queue.size", "8");
        defaultProps.setProperty("http.status.enabled", "true");
        defaultProps.setProperty("http.status.port", "7674");
        defaultProps.setProperty("http.status.bind.address", "0.0.0.0");
        defaultProps.setProperty("http.status.worker.threads", "2");
        defaultProps.setProperty("http.status.queue.size", "32");
        defaultProps.setProperty("server.tps", "60");
        defaultProps.setProperty("server.thread.pool.size", "50");
        defaultProps.setProperty("server.processor.limit", "0");
        defaultProps.setProperty("server.ram.limit", "0");
        defaultProps.setProperty("server.worker.thread.limit", "32");
        defaultProps.setProperty("keystore.path", "keystore.jks");
        defaultProps.setProperty("keystore.validity.days", "365");
        
        try (FileOutputStream fos = new FileOutputStream(CONFIG_FILE)) {
            defaultProps.store(fos, "Proto Nova Server Configuration\n" +
                    "Last generated: " + new java.util.Date() + "\n" +
                    "Edit this file to change server settings. Changes require server restart.");
        }
    }
    
    // Parse all properties from file into memory variables
    private void parseProperties() {
        this.gameSocketPort = getPortProperty("game.socket.port", 7675);
        this.gameSocketIdleTimeoutSeconds = getBoundedPositiveIntProperty("game.socket.idle.timeout.seconds", 300, 86_400);
        this.gameSocketOutboundQueueSize = getBoundedPositiveIntProperty("game.socket.outbound.queue.size", 8, 1_024);
        this.statusHttpEnabled = getBooleanProperty("http.status.enabled", true);
        this.statusHttpPort = getPortProperty("http.status.port", 7674);
        this.statusHttpBindAddress = getStringProperty("http.status.bind.address", "0.0.0.0").trim();
        this.statusHttpWorkerThreads = getBoundedPositiveIntProperty("http.status.worker.threads", 2, 64);
        this.statusHttpQueueSize = getBoundedPositiveIntProperty("http.status.queue.size", 32, 10_000);
        this.ticksPerSecond = getIntProperty("server.tps", 60);
        this.threadPoolSize = getBoundedPositiveIntProperty("server.thread.pool.size", 50, 10_000);
        this.processorLimit = getIntProperty("server.processor.limit", 0);
        this.ramLimit = getIntProperty("server.ram.limit", 0);
        this.workerThreadLimit = getIntProperty("server.worker.thread.limit", 32);
        this.keystorePath = getStringProperty("keystore.path", "keystore.jks");
        this.legacyKeystorePassword = getStringProperty("keystore.password", "");
        this.keystoreValidityDays = getIntProperty("keystore.validity.days", 365);
        
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

    private int getPositiveIntProperty(String key, int defaultValue) {
        int value = getIntProperty(key, defaultValue);
        if (value > 0) return value;
        console.print("WARNING: Property '" + key + "' must be greater than zero; using default: " + defaultValue);
        return defaultValue;
    }

    private int getBoundedPositiveIntProperty(String key, int defaultValue, int maximum) {
        int value = getPositiveIntProperty(key, defaultValue);
        if (value <= maximum) return value;
        console.print("WARNING: Property '" + key + "' cannot exceed " + maximum + "; using default: " + defaultValue);
        return defaultValue;
    }

    private int getPortProperty(String key, int defaultValue) {
        int value = getIntProperty(key, defaultValue);
        if (value >= 1 && value <= 65_535) return value;
        console.print("WARNING: Property '" + key + "' must be between 1 and 65535; using default: " + defaultValue);
        return defaultValue;
    }
    
    // Get string property with default fallback
    private String getStringProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    private boolean getBooleanProperty(String key, boolean defaultValue) {
        String value = properties.getProperty(key);
        if (value == null) return defaultValue;
        if (value.equalsIgnoreCase("true")) return true;
        if (value.equalsIgnoreCase("false")) return false;
        console.print("⚠ Invalid boolean for property '" + key + "', using default: " + defaultValue);
        return defaultValue;
    }
    
    // Port for SSL/TLS encrypted game connections
    public int getGameSocketPort() { return gameSocketPort; }

    // Disconnect authenticated clients that send no data for this many seconds.
    public int getGameSocketIdleTimeoutSeconds() { return gameSocketIdleTimeoutSeconds; }

    // Maximum number of unsent server packets retained for one client.
    public int getGameSocketOutboundQueueSize() { return gameSocketOutboundQueueSize; }
    
    // Optional HTTPS server-status endpoint. Do not expose it unless needed.
    public boolean isStatusHttpEnabled() { return statusHttpEnabled; }

    public int getStatusHttpPort() { return statusHttpPort; }

    public String getStatusHttpBindAddress() {
        return statusHttpBindAddress.isEmpty() ? "0.0.0.0" : statusHttpBindAddress;
    }

    public int getStatusHttpWorkerThreads() { return statusHttpWorkerThreads; }

    public int getStatusHttpQueueSize() { return statusHttpQueueSize; }
    
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
    
    // Migration support for old installations. New installs use an environment variable or password sidecar.
    public String getLegacyKeystorePassword() { return legacyKeystorePassword; }
    
    // Validity period for self-signed certificates in days
    public int getKeystoreValidityDays() { return keystoreValidityDays; }
}
