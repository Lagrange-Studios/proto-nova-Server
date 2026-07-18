package security;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.cert.Certificate;
import org.junit.Test;

public class KeystoreManagerTest {
    @Test
    public void createsUniquePersistentServerIdentities() throws Exception {
        Path directory = Files.createTempDirectory("proto-nova-keystore-test");
        Path firstPath = directory.resolve("first.p12");
        Path secondPath = directory.resolve("second.p12");

        KeystoreManager.LoadedKeystore first = KeystoreManager.loadOrCreate(firstPath, "", 30);
        KeystoreManager.LoadedKeystore reloaded = KeystoreManager.loadOrCreate(firstPath, "", 30);
        KeystoreManager.LoadedKeystore second = KeystoreManager.loadOrCreate(secondPath, "", 30);

        assertTrue(Files.isRegularFile(firstPath));
        assertTrue(first.keyStore.aliases().hasMoreElements());
        String firstAlias = first.keyStore.aliases().nextElement();
        String reloadedAlias = reloaded.keyStore.aliases().nextElement();
        String secondAlias = second.keyStore.aliases().nextElement();
        Certificate firstCertificate = first.keyStore.getCertificate(firstAlias);
        assertArrayEquals(firstCertificate.getEncoded(), reloaded.keyStore.getCertificate(reloadedAlias).getEncoded());
        assertNotEquals(java.util.Base64.getEncoder().encodeToString(firstCertificate.getEncoded()),
                java.util.Base64.getEncoder().encodeToString(second.keyStore.getCertificate(secondAlias).getEncoded()));
    }
}
