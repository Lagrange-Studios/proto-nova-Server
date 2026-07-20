package security;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.cert.Certificate;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
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

    @Test
    public void serializesConcurrentFirstRunCreation() throws Exception {
        Path directory = Files.createTempDirectory("proto-nova-keystore-race-test");
        Path keystorePath = directory.resolve("server.p12");
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<KeystoreManager.LoadedKeystore> first = executor.submit(() -> {
                start.await();
                return KeystoreManager.loadOrCreate(keystorePath, "", 30);
            });
            Future<KeystoreManager.LoadedKeystore> second = executor.submit(() -> {
                start.await();
                return KeystoreManager.loadOrCreate(keystorePath, "", 30);
            });
            start.countDown();

            Certificate firstCertificate = certificate(first.get());
            Certificate secondCertificate = certificate(second.get());
            Certificate reloadedCertificate = certificate(KeystoreManager.loadOrCreate(keystorePath, "", 30));
            assertArrayEquals(firstCertificate.getEncoded(), secondCertificate.getEncoded());
            assertArrayEquals(firstCertificate.getEncoded(), reloadedCertificate.getEncoded());
        } finally {
            executor.shutdownNow();
        }
    }

    private static Certificate certificate(KeystoreManager.LoadedKeystore loaded) throws Exception {
        return loaded.keyStore.getCertificate(loaded.keyStore.aliases().nextElement());
    }
}
