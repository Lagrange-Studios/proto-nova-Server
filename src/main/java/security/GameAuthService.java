package security;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import main.Console;
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

    public String getUsername() {
      return username;
    }

    public String getSessionKey() {
      return sessionKey;
    }
  }

  private final HttpClient httpClient =
      HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
  private final ExecutorService validationExecutor =
      Executors.newFixedThreadPool(
          2,
          runnable -> {
            Thread thread = new Thread(runnable, "security-api-validation");
            thread.setDaemon(true);
            return thread;
          });
  private final String apiUrl;
  private final Console console;

  public GameAuthService(Console console) {
    this.console = console;
    ServerConfig config = ServerConfig.getInstance();
    apiUrl = normalizeBaseUrl(config.getSecurityApiUrl());
  }

  public JoinResult consumeJoinTicket(String ticket, byte[] challenge, boolean continuous) {
    if (ticket == null || ticket.isBlank() || challenge == null || challenge.length != 32)
      return null;

    JsonObject body = new JsonObject();
    body.addProperty("ticket", ticket);
    body.addProperty("challenge", java.util.Base64.getEncoder().encodeToString(challenge));
    body.addProperty("continuous", continuous);

    try {
      HttpResponse<String> response = post(apiUrl + "/game/join-tickets/consume", body);
      if (response.statusCode() != 200) {
        logApiFailure("join-ticket consumption", apiUrl, response);
        return null;
      }
      JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
      String username = json.get("username").getAsString();
      String sessionKey = continuous ? json.get("sessionKey").getAsString() : null;
      if (username.isBlank() || (continuous && (sessionKey == null || sessionKey.isBlank()))) {
        console.print(
            "WARNING: Security API returned an incomplete join-ticket response from " + apiUrl);
        return null;
      }
      return new JoinResult(username, sessionKey);
    } catch (Exception error) {
      logApiException("join-ticket consumption", apiUrl, error);
    }
    return null;
  }

  public CompletableFuture<ValidationStatus> validateSessionAsync(String sessionKey) {
    return CompletableFuture.supplyAsync(() -> validateSession(sessionKey), validationExecutor);
  }

  private ValidationStatus validateSession(String sessionKey) {
    if (sessionKey == null || sessionKey.isBlank()) return ValidationStatus.REJECTED;
    JsonObject body = new JsonObject();
    body.addProperty("sessionKey", sessionKey);

    try {
      HttpResponse<String> response = post(apiUrl + "/game/sessions/validate", body);
      if (isUnavailableStatus(response.statusCode())) {
        logApiFailure("session validation", apiUrl, response);
        return ValidationStatus.UNAVAILABLE;
      }
      if (response.statusCode() != 200) {
        logApiFailure("session validation", apiUrl, response);
        return ValidationStatus.REJECTED;
      }
      JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
      return json.has("valid") && json.get("valid").getAsBoolean()
          ? ValidationStatus.VALID
          : ValidationStatus.REJECTED;
    } catch (Exception error) {
      logApiException("session validation", apiUrl, error);
      return ValidationStatus.UNAVAILABLE;
    }
  }

  private static boolean isUnavailableStatus(int statusCode) {
    return statusCode >= 500
        || statusCode == 404
        || statusCode == 408
        || statusCode == 409
        || statusCode == 425
        || statusCode == 429;
  }

  private void logApiFailure(String operation, String apiUrl, HttpResponse<String> response) {
    String detail = "";
    try {
      JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
      if (json.has("error") && json.get("error").isJsonPrimitive()) {
        detail = ": " + sanitize(json.get("error").getAsString());
      }
    } catch (Exception ignored) {
    }
    console.print(
        "WARNING: Security API "
            + operation
            + " failed at "
            + apiUrl
            + " (HTTP "
            + response.statusCode()
            + ")"
            + detail);
  }

  private void logApiException(String operation, String apiUrl, Exception error) {
    String message = error.getMessage();
    String detail =
        message == null || message.isBlank() ? error.getClass().getSimpleName() : sanitize(message);
    console.print(
        "WARNING: Security API " + operation + " could not reach " + apiUrl + ": " + detail);
  }

  private static String sanitize(String value) {
    String cleaned = value.replace('\r', ' ').replace('\n', ' ').trim();
    return cleaned.length() <= 160 ? cleaned : cleaned.substring(0, 160);
  }

  private HttpResponse<String> post(String url, JsonObject body) throws Exception {
    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(Duration.ofSeconds(7))
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
            .build();
    return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
  }

  private static String normalizeBaseUrl(String value) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("A security API URL is required.");
    }
    String normalized = value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    URI uri = URI.create(normalized);
    boolean local =
        "localhost".equalsIgnoreCase(uri.getHost()) || "127.0.0.1".equals(uri.getHost());
    if (!"https".equalsIgnoreCase(uri.getScheme())
        && !(local && "http".equalsIgnoreCase(uri.getScheme()))) {
      throw new IllegalArgumentException(
          "Security API URLs must use HTTPS unless they point to localhost.");
    }
    return normalized;
  }
}
