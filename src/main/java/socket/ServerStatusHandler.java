package socket;

import com.sun.net.httpserver.HttpsServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.security.KeyStore;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import main.Console;
import org.json.JSONObject;

/**
 * HTTPS server for server status queries and token requests.
 * Allows clients to check server status and request authentication tokens securely.
 */
public class ServerStatusHandler {
    
    private static final int HTTPS_PORT = 7674;
    private ServerSocketHandler socketHandler;
    private Console console;
    private long startTime;
    private HttpsServer httpsServer;
    private TokenManager tokenManager;
    
    public ServerStatusHandler(ServerSocketHandler socketHandler, Console console, TokenManager tokenManager) {
        this.socketHandler = socketHandler;
        this.console = console;
        this.startTime = System.currentTimeMillis();
        this.tokenManager = tokenManager;
    }
    
    /**
     * Start the HTTPS status server
     */
    public void start() throws IOException {
        try {
            // Create HTTPS server with SSL context from keystore
            httpsServer = HttpsServer.create(new InetSocketAddress("0.0.0.0", HTTPS_PORT), 0);
            
            // Load keystore and create SSL context for HTTPS
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            try (java.io.FileInputStream fis = new java.io.FileInputStream("keystore.jks")) {
                keyStore.load(fis, "proto-nova-secure".toCharArray());
            }
            
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(keyStore, "proto-nova-secure".toCharArray());
            
            SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
            sslContext.init(kmf.getKeyManagers(), null, new java.security.SecureRandom());
            
            httpsServer.setHttpsConfigurator(new com.sun.net.httpserver.HttpsConfigurator(sslContext));
            
            httpsServer.createContext("/status", new StatusHandler());
            httpsServer.createContext("/token", new TokenHandler());
            httpsServer.setExecutor(null); // Use default executor
            httpsServer.start();
            console.print("✓ Server HTTPS endpoint listening on port: " + HTTPS_PORT + " (encrypted)");
        } catch (IOException e) {
            console.print("ERROR: Failed to start HTTPS status server on port " + HTTPS_PORT + ": " + e.getMessage());
            throw e;
        } catch (Exception e) {
            console.print("ERROR: Failed to initialize HTTPS: " + e.getMessage());
            throw new IOException(e);
        }
    }
    
    /**
     * Stop the HTTPS status server
     */
    public void stop() {
        if (httpsServer != null) {
            httpsServer.stop(0);
        }
    }
    
    /**
     * Handler for /status endpoint
     */
    private class StatusHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                // Only allow GET requests
                if (!exchange.getRequestMethod().equals("GET")) {
                    sendError(exchange, 405, "Method Not Allowed");
                    return;
                }
                
                // Get server status
                int playerCount = socketHandler.getPlayerList().size();
                long uptimeMillis = System.currentTimeMillis() - startTime;
                long uptimeSeconds = uptimeMillis / 1000;
                long uptimeMinutes = uptimeSeconds / 60;
                long uptimeHours = uptimeMinutes / 60;
                long uptimeDays = uptimeHours / 24;
                
                // Format uptime string
                String uptimeString;
                if (uptimeDays > 0) {
                    uptimeString = uptimeDays + "d " + (uptimeHours % 24) + "h";
                } else if (uptimeHours > 0) {
                    uptimeString = uptimeHours + "h " + (uptimeMinutes % 60) + "m";
                } else if (uptimeMinutes > 0) {
                    uptimeString = uptimeMinutes + "m " + (uptimeSeconds % 60) + "s";
                } else {
                    uptimeString = uptimeSeconds + "s";
                }
                
                // Create JSON response
                JSONObject response = new JSONObject();
                response.put("status", "active");
                response.put("playerCount", playerCount);
                response.put("uptime", uptimeString);
                response.put("uptimeSeconds", uptimeSeconds);
                
                String jsonResponse = response.toString();
                
                // Send response
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                exchange.sendResponseHeaders(200, jsonResponse.getBytes().length);
                
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(jsonResponse.getBytes());
                }
                
            } catch (Exception e) {
                try {
                    sendError(exchange, 500, "Internal Server Error: " + e.getMessage());
                } catch (IOException ignored) {}
                e.printStackTrace();
            }
        }
    }

    /**
     * Handler for /token endpoint - GET to request a new client token
     */
    private class TokenHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                // Only allow GET requests for now
                if (!exchange.getRequestMethod().equals("GET")) {
                    sendError(exchange, 405, "Method Not Allowed");
                    return;
                }
                
                // Generate a new client token
                String clientToken = tokenManager.generateClientToken();
                
                // Create JSON response
                JSONObject response = new JSONObject();
                response.put("token", clientToken);
                response.put("expiresIn", 300); // Token expires in 5 minutes
                
                String jsonResponse = response.toString();
                
                // Send response
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                exchange.sendResponseHeaders(200, jsonResponse.getBytes().length);
                
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(jsonResponse.getBytes());
                }
                
            } catch (Exception e) {
                try {
                    sendError(exchange, 500, "Internal Server Error: " + e.getMessage());
                } catch (IOException ignored) {}
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Send error response
     */
    private void sendError(HttpExchange exchange, int code, String message) throws IOException {
        JSONObject error = new JSONObject();
        error.put("error", message);
        String response = error.toString();
        
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(code, response.getBytes().length);
        
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response.getBytes());
        }
    }
}
