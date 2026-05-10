package socket;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import main.Console;

/**
 * Simple HTTP server to serve the server certificate to clients.
 * Runs on port 7674 (one port below the game socket).
 * This allows clients on remote machines to download the certificate.
 */
public class CertificateServer {
    
    private int CERT_SERVER_PORT;
    private HttpServer httpServer;
    private Console console;
    
    public CertificateServer(Console console) {
        this.console = console;
        this.CERT_SERVER_PORT = main.ServerConfig.getInstance().getCertificateServerPort();
    }
    
    /**
     * Starts the HTTP server for certificate distribution
     */
    public void start() throws IOException {
        try {
            // Ensure certificate is extracted first
            try {
                CertificateExtractor.ensureCertificateExists();
            } catch (Exception e) {
                console.print("✗ Warning: Could not extract certificate: " + e.getMessage());
                console.print("  Certificate server will not be available for remote clients");
                return;
            }
            
            // Create HTTP server with socket reuse enabled
            httpServer = HttpServer.create(new InetSocketAddress("0.0.0.0", CERT_SERVER_PORT), 0);
            httpServer.setExecutor(null); // Set before starting
            
            // Enable socket address reuse to handle TIME_WAIT states
            try {
                java.lang.reflect.Field f = httpServer.getClass().getDeclaredField("impl");
                f.setAccessible(true);
                Object impl = f.get(httpServer);
                java.lang.reflect.Method m = impl.getClass().getDeclaredMethod("setReuseAddress", boolean.class);
                m.setAccessible(true);
                m.invoke(impl, true);
            } catch (Exception e) {
                // If reflection fails, just continue - not critical
            }
            
            // Register endpoint for certificate download
            httpServer.createContext("/certificate", new CertificateHandler());
            httpServer.createContext("/health", new HealthCheckHandler());
            
            httpServer.start();
            
            console.print("✓ Certificate server started on port " + CERT_SERVER_PORT);
            console.print("  Clients can download certificate from: http://<server-ip>:" + CERT_SERVER_PORT + "/certificate");
        } catch (java.net.BindException e) {
            console.print("✗ Failed to start certificate server: Port " + CERT_SERVER_PORT + " is already in use");
            console.print("  Try again in a few seconds, or kill the process using this port");
        } catch (Exception e) {
            console.print("✗ Failed to start certificate server: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Handler for /certificate endpoint - serves the certificate file
     */
    private static class CertificateHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                String method = exchange.getRequestMethod();
                
                // Only allow GET requests
                if (!method.equals("GET")) {
                    exchange.sendResponseHeaders(405, -1); // Method Not Allowed
                    return;
                }
                
                // Read certificate file
                File certFile = new File(CertificateExtractor.getCertificateFilePath());
                if (!certFile.exists()) {
                    exchange.sendResponseHeaders(404, -1); // Not Found
                    return;
                }
                
                // Read file content
                byte[] certData = new byte[(int) certFile.length()];
                try (FileInputStream fis = new FileInputStream(certFile)) {
                    fis.read(certData);
                }
                
                // Send response
                exchange.getResponseHeaders().set("Content-Type", "application/x-x509-ca-cert");
                exchange.getResponseHeaders().set("Content-Length", String.valueOf(certData.length));
                exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*"); // Allow any origin
                exchange.sendResponseHeaders(200, certData.length);
                
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(certData);
                    os.flush();
                }
                
                System.out.println("[CertServer] Certificate downloaded by: " + exchange.getRemoteAddress());
            } catch (Exception e) {
                System.err.println("[CertServer] Error handling certificate request: " + e.getMessage());
                try {
                    exchange.sendResponseHeaders(500, -1); // Internal Server Error
                } catch (IOException ignored) {}
            }
        }
    }
    
    /**
     * Handler for /health endpoint - simple health check
     */
    private static class HealthCheckHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                String response = "OK";
                exchange.getResponseHeaders().set("Content-Type", "text/plain");
                exchange.sendResponseHeaders(200, response.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                    os.flush();
                }
            } catch (Exception e) {
                try {
                    exchange.sendResponseHeaders(500, -1);
                } catch (IOException ignored) {}
            }
        }
    }
    
    /**
     * Stops the HTTP server
     */
    public void stop() {
        if (httpServer != null) {
            httpServer.stop(0);
            console.print("Certificate server stopped");
        }
    }
}
