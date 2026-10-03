package security;

import static org.junit.Assert.*;

import java.nio.file.*;
import org.junit.Test;

public class AutomaticCertificateUpdateTest {
  @Test
  public void enrollmentPersistsAndRejectsInvalidConfiguration() throws Exception {
    Path dir = Files.createTempDirectory("certificate-enrollment-");
    String old = System.getProperty("protonova.tlsRequestDir");
    try {
      System.setProperty("protonova.tlsRequestDir", dir.resolve("tls-request").toString());
      Path input = dir.resolve("enrollment.json");
      Files.writeString(
          input,
          "{\"serverId\":\"012345678901234567890123\",\"hostname\":\"example.servers.proto-nova.net\",\"token\":\""
              + "a".repeat(43)
              + "\"}");
      AutomaticCertificateUpdate.enroll(input);
      assertEquals(
          Files.readString(input), Files.readString(AutomaticCertificateUpdate.settingsPath()));
      String existing = Files.readString(AutomaticCertificateUpdate.settingsPath());
      Files.writeString(
          input, "{\"serverId\":\"bad\",\"hostname\":\"evil.example\",\"token\":\"short\"}");
      assertThrows(IllegalArgumentException.class, () -> AutomaticCertificateUpdate.enroll(input));
      assertEquals(existing, Files.readString(AutomaticCertificateUpdate.settingsPath()));
    } finally {
      if (old == null) System.clearProperty("protonova.tlsRequestDir");
      else System.setProperty("protonova.tlsRequestDir", old);
      try (var paths = Files.walk(dir)) {
        for (Path p : paths.sorted(java.util.Comparator.reverseOrder()).toList())
          Files.deleteIfExists(p);
      }
    }
  }
}
