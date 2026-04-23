package socket;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import main.Console;
import org.json.JSONObject;

/**
 * HTTP server for server status queries.
 * Allows clients to check server status: player count, uptime, etc.
 */
public class ServerStatusHandler {
    
    private static final int HTTP_PORT = 7674;
    private ServerSocketHandler socketHandler;
    private Console console;
    private long startTime;
    private HttpServer httpServer;
    
    public ServerStatusHandler(ServerSocketHandler socketHandler, Console console) {
        this.socketHandler = socketHandler;
        this.console = console;
        this.startTime = System.currentTimeMillis();
    }
    
    /**
     * Start the HTTP status server
     */
    public void start() throws IOException {
        try {
            httpServer = HttpServer.create(new InetSocketAddress("0.0.0.0", HTTP_PORT), 0);
            httpServer.createContext("/status", new StatusHandler());
            httpServer.setExecutor(null); // Use default executor
            httpServer.start();
            console.print("✓ Server status HTTP endpoint listening on port: " + HTTP_PORT);
        } catch (IOException e) {
            console.print("ERROR: Failed to start HTTP status server on port " + HTTP_PORT + ": " + e.getMessage());
            throw e;
        }
    }
    
    /**
     * Stop the HTTP status server
     */
    public void stop() {
        if (httpServer != null) {
            httpServer.stop(0);
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
