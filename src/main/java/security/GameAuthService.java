package security;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import main.ServerConfig;

public final class GameAuthService {

    public enum ValidationStatus {
        VALID,
        REJECTED,
        UNAVAILABLE
    }

    public static final class JoinResult {
        private final String username;
        private final String sessionKey;

        private JoinResult(String username, String sessionKey) {
            this.username = username;
            this.sessionKey = sessionKey;
        }

        public String getUsername() { return username; }

        public String getSessionKey() { return sessionKey; }
    }

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    private final String[] apiUrls;

    public GameAuthService() {
        ServerConfig config = ServerConfig.getInstance();
        apiUrls = new String[] {
                normalizeBaseUrl(config.getSecurityApiUrl()),
                normalizeBaseUrl(config.getSecurityApiFallbackUrl())
        };
    }

    public JoinResult consumeJoinTicket(String ticket, byte[] challenge, boolean continuous) {
        if (ticket == null || ticket.isBlank() || challenge == null || challenge.length != 32) return null;

        JsonObject body = new JsonObject();
        body.addProperty("ticket", ticket);
        body.addProperty("challenge", java.util.Base64.getEncoder().encodeToString(challenge));
        body.addProperty("continuous", continuous);

        for (String apiUrl : apiUrls) {
            if (apiUrl == null) continue;
            try {
                HttpResponse<String> response = post(apiUrl + "/game/join-tickets/consume", body);
                if (response.statusCode() >= 500) continue;
                if (response.statusCode() != 200) return null;
                JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
                String username = json.get("username").getAsString();
                String sessionKey = continuous ? json.get("sessionKey").getAsString() : null;
                if (username.isBlank() || (continuous && (sessionKey == null || sessionKey.isBlank()))) return null;
                return new JoinResult(username, sessionKey);
            } catch (Exception ignored) {}
        }
        return null;
    }

    public CompletableFuture<ValidationStatus> validateSessionAsync(String sessionKey) {
        return CompletableFuture.supplyAsync(() -> validateSession(sessionKey));
    }

    private ValidationStatus validateSession(String sessionKey) {
        if (sessionKey == null || sessionKey.isBlank()) return ValidationStatus.REJECTED;
        JsonObject body = new JsonObject();
        body.addProperty("sessionKey", sessionKey);

        boolean reachedApi = false;
        for (String apiUrl : apiUrls) {
            if (apiUrl == null) continue;
            try {
                HttpResponse<String> response = post(apiUrl + "/game/sessions/validate", body);
                if (response.statusCode() >= 500) continue;
                reachedApi = true;
                if (response.statusCode() != 200) return ValidationStatus.REJECTED;
                JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
                return json.has("valid") && json.get("valid").getAsBoolean()
                        ? ValidationStatus.VALID : ValidationStatus.REJECTED;
            } catch (Exception ignored) {}
        }
        return reachedApi ? ValidationStatus.REJECTED : ValidationStatus.UNAVAILABLE;
    }

    private HttpResponse<String> post(String url, JsonObject body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(7))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private static String normalizeBaseUrl(String value) {
        if (value == null || value.isBlank()) return null;
        String normalized = value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
        URI uri = URI.create(normalized);
        boolean local = "localhost".equalsIgnoreCase(uri.getHost()) || "127.0.0.1".equals(uri.getHost());
        if (!"https".equalsIgnoreCase(uri.getScheme()) && !(local && "http".equalsIgnoreCase(uri.getScheme()))) {
            throw new IllegalArgumentException("Security API URLs must use HTTPS unless they point to localhost.");
        }
        return normalized;
    }
}
