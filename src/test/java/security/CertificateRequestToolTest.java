package security;

import static org.junit.Assert.*;

import java.nio.file.Files;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.operator.jcajce.JcaContentVerifierProviderBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class CertificateRequestToolTest {
  @Rule public TemporaryFolder files = new TemporaryFolder();

  @Test
  public void createsSignedRequestAndKeepsPrivateKeyOnRenewal() throws Exception {
    var directory = files.newFolder().toPath();
    var requestPath = CertificateRequestTool.create("test.servers.proto-nova.net", directory);
    var originalKey = Files.readAllBytes(directory.resolve("privkey.pem"));
    try (var parser = new PEMParser(Files.newBufferedReader(requestPath))) {
      var request = (PKCS10CertificationRequest) parser.readObject();
      assertEquals("CN=test.servers.proto-nova.net", request.getSubject().toString());
      assertTrue(
          request.isSignatureValid(
              new JcaContentVerifierProviderBuilder().build(request.getSubjectPublicKeyInfo())));
    }
    CertificateRequestTool.create("test.servers.proto-nova.net", directory);
    assertArrayEquals(originalKey, Files.readAllBytes(directory.resolve("privkey.pem")));
    assertFalse(Files.readString(requestPath).contains("PRIVATE KEY"));
  }

  @Test
  public void rejectsWildcardAndInvalidHostname() throws Exception {
    var directory = files.newFolder().toPath();
    for (String hostname :
        new String[] {"*.proto-nova.net", "test.proto-nova.net:7675", "bad,name.proto-nova.net"}) {
      assertThrows(
          IllegalArgumentException.class, () -> CertificateRequestTool.create(hostname, directory));
    }
    assertFalse(Files.exists(directory.resolve("privkey.pem")));
  }
}
