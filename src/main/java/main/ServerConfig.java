package main;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class ServerConfig {
    
    private static final Path CONFIG_FILE = Path.of(
            System.getProperty("protonova.configFile", "proto-nova.properties")).toAbsolutePath().normalize();
    private static ServerConfig instance;
    private Properties properties;
    private Console console;
    
    private int gameSocketPort;
    private int gameSocketIdleTimeoutSeconds;
    private int gameSocketOutboundQueueSize;
    private int gameSocketSlowClientTimeoutSeconds;
    private int securityLevel;
    private String securityApiUrl;
    private String securityApiFallbackUrl;
    private int securityLevel3RecheckSeconds;
    private int securityLevel3ApiGraceSeconds;
    private boolean statusHttpEnabled;
    private int statusHttpPort;
    private String statusHttpBindAddress;
    private int statusHttpWorkerThreads;
    private int statusHttpQueueSize;
    private String clientDistributionDirectory;
    private int ticksPerSecond;
    private int threadPoolSize;
    private int processorLimit;
    private int ramLimit;
    private int workerThreadLimit;
    private boolean cataclysmEnabled;
    private int headlessStatusIntervalSeconds;
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
        File configFile = CONFIG_FILE.toFile();
        
        if (!configFile.exists()) {
            createDefaultConfig();
        }
        
        try (FileInputStream fis = new FileInputStream(configFile)) {
            properties.load(fis);
        }
        
    }
    
    // Create default configuration file with all settings
    private void createDefaultConfig() throws IOException {
        Path parent = CONFIG_FILE.getParent();
        if (parent != null) Files.createDirectories(parent);
        String defaults = "# Proto Nova Server Configuration\n"
                + "# Run the server with --check-config after editing. Changes require a restart.\n\n"
                + "# Secure game listener. Players enter this one base port.\n"
                + "game.socket.port=7675\n"
                + "game.socket.idle.timeout.seconds=300\n"
                + "game.socket.outbound.queue.size=8\n"
                + "game.socket.slow.client.timeout.seconds=15\n\n"
                + "# Player authentication/security level: 1, 2, or 3.\n"
                + "security.level=1\n"
                + "security.api.url=https://api.proto-nova.net/api\n"
                + "security.api.fallback.url=https://proto-nova-api.up.railway.app/api\n"
                + "security.level3.recheck.seconds=60\n"
                + "security.level3.api.grace.seconds=180\n\n"
                + "# HTTPS status and signed client-download listener uses game.socket.port - 1.\n"
                + "# For example, base game port 8125 automatically uses companion port 8124.\n"
                + "http.status.enabled=true\n"
                + "http.status.bind.address=0.0.0.0\n"
                + "http.status.worker.threads=2\n"
                + "http.status.queue.size=32\n"
                + "client.distribution.directory=client-distribution\n\n"
                + "# Gameplay and performance. Zero resource limits mean automatic/unlimited.\n"
                + "game.cataclysm.enabled=true\n"
                + "server.tps=20\n"
                + "server.thread.pool.size=50\n"
                + "server.processor.limit=0\n"
                + "server.ram.limit=0\n"
                + "server.worker.thread.limit=32\n"
                + "headless.status.interval.seconds=60\n\n"
                + "# The server creates and protects its TLS keystore automatically.\n"
                + "keystore.path=keystore.jks\n"
                + "keystore.validity.days=365\n";
        Files.writeString(CONFIG_FILE, defaults, StandardCharsets.UTF_8);
    }
    
    // Parse all properties from file into memory variables
    private void parseProperties() {
        this.gameSocketPort = getPortProperty("game.socket.port", 7675);
        this.gameSocketIdleTimeoutSeconds = getBoundedPositiveIntProperty("game.socket.idle.timeout.seconds", 300, 86_400);
        this.gameSocketOutboundQueueSize = getBoundedPositiveIntProperty("game.socket.outbound.queue.size", 8, 1_024);
        this.gameSocketSlowClientTimeoutSeconds = getBoundedPositiveIntProperty(
                "game.socket.slow.client.timeout.seconds", 15, 300);
        this.securityLevel = getSecurityLevelProperty();
        this.securityApiUrl = getStringProperty("security.api.url", "https://api.proto-nova.net/api").trim();
        this.securityApiFallbackUrl = getStringProperty(
                "security.api.fallback.url", "https://proto-nova-api.up.railway.app/api").trim();
        this.securityLevel3RecheckSeconds = getBoundedPositiveIntProperty(
                "security.level3.recheck.seconds", 60, 3_600);
        this.securityLevel3ApiGraceSeconds = getBoundedPositiveIntProperty(
                "security.level3.api.grace.seconds", 180, 86_400);
        this.statusHttpEnabled = getBooleanProperty("http.status.enabled", true);
        this.statusHttpPort = companionPort(this.gameSocketPort);
        String legacyStatusPort = properties.getProperty("http.status.port");
        if (legacyStatusPort != null && !legacyStatusPort.isBlank()
                && !legacyStatusPort.equalsIgnoreCase("auto")) {
            console.print("NOTICE: 'http.status.port' is no longer configured separately. "
                    + "Using companion port " + statusHttpPort + " from base game port " + gameSocketPort + ".");
        }
        this.statusHttpBindAddress = getStringProperty("http.status.bind.address", "0.0.0.0").trim();
        this.statusHttpWorkerThreads = getBoundedPositiveIntProperty("http.status.worker.threads", 2, 64);
        this.statusHttpQueueSize = getBoundedPositiveIntProperty("http.status.queue.size", 32, 10_000);
        this.clientDistributionDirectory = getStringProperty(
                "client.distribution.directory", "client-distribution").trim();
        this.ticksPerSecond = getIntProperty("server.tps", 20);
        this.threadPoolSize = getBoundedPositiveIntProperty("server.thread.pool.size", 50, 10_000);
        this.processorLimit = getIntProperty("server.processor.limit", 0);
        this.ramLimit = getIntProperty("server.ram.limit", 0);
        this.workerThreadLimit = getIntProperty("server.worker.thread.limit", 32);
        this.cataclysmEnabled = getBooleanProperty("game.cataclysm.enabled", true);
        this.headlessStatusIntervalSeconds = getBoundedPositiveIntProperty(
                "headless.status.interval.seconds", 60, 3_600);
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
            if (Boolean.getBoolean("protonova.strictConfig")) {
                throw new IllegalArgumentException("Property '" + key + "' must be an integer.");
            }
            console.print("⚠ Invalid integer for property '" + key + "', using default: " + defaultValue);
            return defaultValue;
        }
    }

    private int getPositiveIntProperty(String key, int defaultValue) {
        int value = getIntProperty(key, defaultValue);
        if (value > 0) return value;
        if (Boolean.getBoolean("protonova.strictConfig")) {
            throw new IllegalArgumentException("Property '" + key + "' must be greater than zero.");
        }
        console.print("WARNING: Property '" + key + "' must be greater than zero; using default: " + defaultValue);
        return defaultValue;
    }

    private int getBoundedPositiveIntProperty(String key, int defaultValue, int maximum) {
        int value = getPositiveIntProperty(key, defaultValue);
        if (value <= maximum) return value;
        if (Boolean.getBoolean("protonova.strictConfig")) {
            throw new IllegalArgumentException("Property '" + key + "' cannot exceed " + maximum + ".");
        }
        console.print("WARNING: Property '" + key + "' cannot exceed " + maximum + "; using default: " + defaultValue);
        return defaultValue;
    }

    private int getPortProperty(String key, int defaultValue) {
        int value = getIntProperty(key, defaultValue);
        if (value >= 2 && value <= 65_535) return value;
        if (Boolean.getBoolean("protonova.strictConfig")) {
            throw new IllegalArgumentException("Property '" + key + "' must be between 2 and 65535.");
        }
        console.print("WARNING: Property '" + key + "' must be between 2 and 65535; using default: " + defaultValue);
        return defaultValue;
    }

    private int getSecurityLevelProperty() {
        int value = getIntProperty("security.level", 1);
        if (value >= 1 && value <= 3) return value;
        if (Boolean.getBoolean("protonova.strictConfig")) {
            throw new IllegalArgumentException("Property 'security.level' must be 1, 2, or 3.");
        }
        console.print("WARNING: Property 'security.level' must be 1, 2, or 3; using default: 1");
        return 1;
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
        if (Boolean.getBoolean("protonova.strictConfig")) {
            throw new IllegalArgumentException("Property '" + key + "' must be true or false.");
        }
        console.print("⚠ Invalid boolean for property '" + key + "', using default: " + defaultValue);
        return defaultValue;
    }
    
    // Port for SSL/TLS encrypted game connections
    public int getGameSocketPort() { return gameSocketPort; }

    // Disconnect authenticated clients that send no data for this many seconds.
    public int getGameSocketIdleTimeoutSeconds() { return gameSocketIdleTimeoutSeconds; }

    // Minimum number of unsent server packets retained for one client.
    public int getGameSocketOutboundQueueSize() { return gameSocketOutboundQueueSize; }

    // Disconnect a client after this many seconds of outbound packets are backlogged.
    public int getGameSocketSlowClientTimeoutSeconds() { return gameSocketSlowClientTimeoutSeconds; }

    public int getSecurityLevel() { return securityLevel; }

    public String getSecurityApiUrl() { return securityApiUrl; }

    public String getSecurityApiFallbackUrl() { return securityApiFallbackUrl; }

    public int getSecurityLevel3RecheckSeconds() { return securityLevel3RecheckSeconds; }

    public int getSecurityLevel3ApiGraceSeconds() { return securityLevel3ApiGraceSeconds; }
    
    // Optional HTTPS server-status endpoint. Do not expose it unless needed.
    public boolean isStatusHttpEnabled() { return statusHttpEnabled; }

    public int getStatusHttpPort() { return statusHttpPort; }

    /** Companion HTTPS/download port derived from the only user-facing base port. */
    public static int companionPort(int baseGamePort) {
        if (baseGamePort < 2 || baseGamePort > 65_535) {
            throw new IllegalArgumentException("Base game port must be between 2 and 65535.");
        }
        return baseGamePort - 1;
    }

    public String getStatusHttpBindAddress() {
        return statusHttpBindAddress.isEmpty() ? "0.0.0.0" : statusHttpBindAddress;
    }

    public int getStatusHttpWorkerThreads() { return statusHttpWorkerThreads; }

    public int getStatusHttpQueueSize() { return statusHttpQueueSize; }

    /** Directory containing the signed manifest.json and client.zip served to launchers. */
    public String getClientDistributionDirectory() {
        return clientDistributionDirectory.isEmpty() ? "client-distribution" : clientDistributionDirectory;
    }
    
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

    // Disable this for an open-ended survival server with no cataclysm events.
    public boolean isCataclysmEnabled() { return cataclysmEnabled; }

    public int getHeadlessStatusIntervalSeconds() { return headlessStatusIntervalSeconds; }

    public static Path getConfigPath() { return CONFIG_FILE; }
    
    // Path to SSL keystore file
    public String getKeystorePath() { return keystorePath; }
    
    // Migration support for old installations. New installs use an environment variable or password sidecar.
    public String getLegacyKeystorePassword() { return legacyKeystorePassword; }
    
    // Validity period for self-signed certificates in days
    public int getKeystoreValidityDays() { return keystoreValidityDays; }
}
