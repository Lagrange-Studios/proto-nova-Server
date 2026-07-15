package socket;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import main.Console;
import diagnostics.ResourceDiagnostics;
import org.json.JSONObject;

/**
 * Optional HTTP server for non-sensitive status queries.
 */
public class ServerStatusHandler {
    
    private int HTTP_PORT;
    private ServerSocketHandler socketHandler;
    private Console console;
    private long startTime;
    private HttpServer httpServer;
    private ExecutorService httpExecutor;
    
    public ServerStatusHandler(ServerSocketHandler socketHandler, Console console) {
        this.socketHandler = socketHandler;
        this.console = console;
        this.startTime = System.currentTimeMillis();
        this.HTTP_PORT = main.ServerConfig.getInstance().getStatusHttpPort();
    }
    
    /**
     * Start the HTTP status server
     */
    public void start() throws IOException {
        try {
            // Create simple HTTP server (no SSL needed for status endpoints)
            httpServer = HttpServer.create(new InetSocketAddress("0.0.0.0", HTTP_PORT), 0);
            
            httpServer.createContext("/status", new StatusHandler());
            httpExecutor = Executors.newCachedThreadPool(ResourceDiagnostics.threadFactory("Status-HTTP-Worker"));
            httpServer.setExecutor(httpExecutor);
            httpServer.start();
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException(e);
        }
    }
    
    /**
     * Stop the HTTP status server
     */
    public void stop() {
        if (httpServer != null) {
            httpServer.stop(0);
        }
        if (httpExecutor != null) {
            httpExecutor.shutdownNow();
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
                    sendError(exchange, 500, "Internal Server Error");
                } catch (IOException ignored) {}
                console.print("WARNING: A status request failed.");
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
