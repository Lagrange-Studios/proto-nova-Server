package security;

import com.google.gson.JsonParser;
import java.net.URI;
import java.net.http.*;
import java.nio.file.*;
import java.security.cert.*;
import java.time.Duration;
import java.util.concurrent.*;
import javax.net.ssl.*;

/** Downloads only the public certificate using a narrowly scoped enrollment token. */
public final class AutomaticCertificateUpdate {
  private static boolean started;

  private AutomaticCertificateUpdate() {}

  public static Path settingsPath() {
    return Path.of(
        System.getProperty("protonova.tlsRequestDir", "tls-request"), "auto-update.json");
  }

  public static void enroll(Path source) throws Exception {
    var json = JsonParser.parseString(Files.readString(source)).getAsJsonObject();
    if (!json.get("serverId").getAsString().matches("[a-fA-F0-9]{24}")
        || !json.get("hostname").getAsString().matches("[a-z0-9-]+\\.servers\\.proto-nova\\.net")
        || !json.get("token").getAsString().matches("[A-Za-z0-9_-]{43}"))
      throw new IllegalArgumentException("Invalid certificate enrollment file.");
    Path target = settingsPath();
    Files.createDirectories(target.toAbsolutePath().getParent());
    if (!source.toAbsolutePath().normalize().equals(target.toAbsolutePath().normalize()))
      Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
    try {
      Files.setPosixFilePermissions(
          target, java.nio.file.attribute.PosixFilePermissions.fromString("rw-------"));
    } catch (UnsupportedOperationException ignored) {
    }
    System.out.println("Automatic certificate updates enrolled. Keep " + target + " private.");
  }

  public static synchronized void start(Path certificate, Path key) {
    if (started || !Files.isRegularFile(settingsPath())) return;
    started = true;
    var timer =
        Executors.newSingleThreadScheduledExecutor(
            task -> {
              Thread t = new Thread(task, "Certificate-Update");
              t.setDaemon(true);
              return t;
            });
    timer.scheduleWithFixedDelay(
        () -> {
          try {
            sync(certificate, key);
          } catch (Exception e) {
            System.err.println(
                "Automatic certificate update failed; retaining the current certificate ("
                    + e.getClass().getSimpleName()
                    + ").");
          }
        },
        10,
        3600,
        TimeUnit.SECONDS);
  }

  static void sync(Path certificate, Path key) throws Exception {
    var cfg = JsonParser.parseString(Files.readString(settingsPath())).getAsJsonObject();
    String id = cfg.get("serverId").getAsString(),
        host = cfg.get("hostname").getAsString(),
        token = cfg.get("token").getAsString();
    if (!id.matches("[a-fA-F0-9]{24}") || !token.matches("[A-Za-z0-9_-]{43}"))
      throw new IllegalArgumentException("Invalid enrollment.");
    var client =
        HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();
    var request =
        HttpRequest.newBuilder(URI.create("https://api.proto-nova.net/api/certificate-sync/" + id))
            .timeout(Duration.ofSeconds(30))
            .header("Authorization", "Bearer " + token)
            .GET()
            .build();
    var response = client.send(request, HttpResponse.BodyHandlers.ofString());
    if (response.statusCode() != 200 || response.body().length() > 50000)
      throw new IllegalStateException("Certificate sync unavailable.");
    if (Files.exists(certificate) && Files.readString(certificate).equals(response.body())) return;
    Path staged =
        Files.createTempFile(certificate.toAbsolutePath().getParent(), "certificate-", ".pem");
    try {
      Files.writeString(staged, response.body());
      var loaded = PemCertificateLoader.load(staged, key);
      var chain =
          java.util.Arrays.stream(loaded.keyStore.getCertificateChain("proto-nova-server"))
              .map(c -> (X509Certificate) c)
              .toArray(X509Certificate[]::new);
      var names = chain[0].getSubjectAlternativeNames();
      if (names == null
          || names.stream()
              .noneMatch(n -> Integer.valueOf(2).equals(n.get(0)) && host.equals(n.get(1))))
        throw new CertificateException("Hostname mismatch.");
      var factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
      factory.init((java.security.KeyStore) null);
      for (var manager : factory.getTrustManagers())
        if (manager instanceof X509TrustManager trust)
          trust.checkServerTrusted(chain, chain[0].getPublicKey().getAlgorithm());
      try {
        Files.move(
            staged,
            certificate,
            StandardCopyOption.ATOMIC_MOVE,
            StandardCopyOption.REPLACE_EXISTING);
      } catch (AtomicMoveNotSupportedException e) {
        Files.move(staged, certificate, StandardCopyOption.REPLACE_EXISTING);
      }
      java.util.Arrays.fill(loaded.password, '\0');
      System.out.println(
          "Updated trusted certificate for " + host + "; new connections use it automatically.");
    } finally {
      Files.deleteIfExists(staged);
    }
  }
}
