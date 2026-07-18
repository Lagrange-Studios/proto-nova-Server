package socket;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLContext;
import main.Console;
import diagnostics.ResourceDiagnostics;
import org.json.JSONObject;

/**
 * Optional HTTPS server for non-sensitive status queries.
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
     * Start the HTTPS status server
     */
    public void start() throws IOException {
        try {
            String bindAddress = main.ServerConfig.getInstance().getStatusHttpBindAddress();
            SSLContext sslContext = security.ServerTlsContext.create();
            HttpsServer httpsServer = HttpsServer.create(new InetSocketAddress(bindAddress, HTTP_PORT), 16);
            httpsServer.setHttpsConfigurator(new HttpsConfigurator(sslContext));
            httpServer = httpsServer;
            
            httpServer.createContext("/status", new StatusHandler());
            int workerThreads = main.ServerConfig.getInstance().getStatusHttpWorkerThreads();
            int queueSize = main.ServerConfig.getInstance().getStatusHttpQueueSize();
            httpExecutor = new ThreadPoolExecutor(workerThreads, workerThreads, 0L, TimeUnit.MILLISECONDS,
                    new ArrayBlockingQueue<>(queueSize),
                    ResourceDiagnostics.threadFactory("Status-HTTP-Worker"),
                    new ThreadPoolExecutor.CallerRunsPolicy());
            httpServer.setExecutor(httpExecutor);
            httpServer.start();
            console.print("Status listener active on https://" + bindAddress + ":" + HTTP_PORT + "/status");
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException(e);
        }
    }
    
    /**
     * Stop the HTTPS status server
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
                
                byte[] jsonResponse = response.toString().getBytes(StandardCharsets.UTF_8);
                
                // Send response
                exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
                exchange.getResponseHeaders().set("Connection", "close");
                exchange.sendResponseHeaders(200, jsonResponse.length);
                
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(jsonResponse);
                }
                
            } catch (Exception e) {
                try {
                    sendError(exchange, 500, "Internal Server Error");
                } catch (IOException ignored) {}
                console.print("WARNING: A status request failed.");
            } finally {
                exchange.close();
            }
        }
    }

    /**
     * Send error response
     */
    private void sendError(HttpExchange exchange, int code, String message) throws IOException {
        JSONObject error = new JSONObject();
        error.put("error", message);
        byte[] response = error.toString().getBytes(StandardCharsets.UTF_8);
        
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.getResponseHeaders().set("Connection", "close");
        exchange.sendResponseHeaders(code, response.length);
        
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response);
        }
    }
}
