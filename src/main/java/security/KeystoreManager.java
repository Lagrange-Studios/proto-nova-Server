package security;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import java.util.EnumSet;

import main.ServerConfig;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.ExtendedKeyUsage;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

/** Owns the server's private key and creates a unique certificate on first run. */
final class KeystoreManager {
    private static final String ALIAS = "proto-nova-server";
    private static final String TYPE = "PKCS12";
    private static final String PASSWORD_ENV = "PROTO_NOVA_KEYSTORE_PASSWORD";
    private static final String LEGACY_PASSWORD = "proto-nova-secure";

    private KeystoreManager() {}

    static LoadedKeystore loadOrCreate() throws Exception {
        Path keystorePath = Paths.get(ServerConfig.getInstance().getKeystorePath()).toAbsolutePath().normalize();
        return loadOrCreate(keystorePath, ServerConfig.getInstance().getLegacyKeystorePassword(),
                ServerConfig.getInstance().getKeystoreValidityDays());
    }

    static synchronized LoadedKeystore loadOrCreate(Path keystorePath, String legacyPassword, int validityDays) throws Exception {
        Path passwordPath = Paths.get(keystorePath.toString() + ".password");
        Path parent = keystorePath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        if (Files.exists(keystorePath)) {
            char[] password = resolveExistingPassword(passwordPath, legacyPassword);
            return new LoadedKeystore(load(keystorePath, password), password);
        }

        char[] password = resolveNewPassword(passwordPath);
        KeyStore keyStore = create(password, validityDays);
        try (OutputStream output = Files.newOutputStream(keystorePath)) {
            keyStore.store(output, password);
        }
        restrictToOwner(keystorePath);
        return new LoadedKeystore(keyStore, password);
    }

    private static KeyStore load(Path path, char[] password) throws Exception {
        KeyStore keyStore = KeyStore.getInstance(TYPE);
        try (InputStream input = Files.newInputStream(path)) {
            keyStore.load(input, password);
        }
        return keyStore;
    }

    private static char[] resolveExistingPassword(Path passwordPath, String legacyPassword) throws IOException {
        String environmentPassword = System.getenv(PASSWORD_ENV);
        if (environmentPassword != null && !environmentPassword.isBlank()) {
            return environmentPassword.toCharArray();
        }
        if (Files.exists(passwordPath)) {
            return Files.readString(passwordPath, StandardCharsets.UTF_8).trim().toCharArray();
        }
        return (legacyPassword.isEmpty() ? LEGACY_PASSWORD : legacyPassword).toCharArray();
    }

    private static char[] resolveNewPassword(Path passwordPath) throws IOException {
        String environmentPassword = System.getenv(PASSWORD_ENV);
        if (environmentPassword != null && !environmentPassword.isBlank()) {
            return environmentPassword.toCharArray();
        }
        byte[] random = new byte[32];
        new SecureRandom().nextBytes(random);
        String password = Base64.getUrlEncoder().withoutPadding().encodeToString(random);
        Files.writeString(passwordPath, password, StandardCharsets.UTF_8);
        restrictToOwner(passwordPath);
        return password.toCharArray();
    }

    private static KeyStore create(char[] password, int validityDays) throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(3072, new SecureRandom());
        KeyPair keyPair = generator.generateKeyPair();

        Instant now = Instant.now();
        X500Name name = new X500Name("CN=Proto Nova Game Server");
        BigInteger serial = new BigInteger(160, new SecureRandom()).abs();
        JcaX509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(
                name, serial, Date.from(now.minus(5, ChronoUnit.MINUTES)),
                Date.from(now.plus(validityDays, ChronoUnit.DAYS)),
                name, keyPair.getPublic());
        JcaX509ExtensionUtils extensions = new JcaX509ExtensionUtils();
        builder.addExtension(Extension.basicConstraints, true, new BasicConstraints(false));
        builder.addExtension(Extension.keyUsage, true,
                new KeyUsage(KeyUsage.digitalSignature | KeyUsage.keyEncipherment));
        builder.addExtension(Extension.extendedKeyUsage, false,
                new ExtendedKeyUsage(KeyPurposeId.id_kp_serverAuth));
        builder.addExtension(Extension.subjectKeyIdentifier, false,
                extensions.createSubjectKeyIdentifier(keyPair.getPublic()));
        builder.addExtension(Extension.authorityKeyIdentifier, false,
                extensions.createAuthorityKeyIdentifier(keyPair.getPublic()));

        ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA").build(keyPair.getPrivate());
        X509CertificateHolder holder = builder.build(signer);
        X509Certificate certificate = new JcaX509CertificateConverter().getCertificate(holder);
        certificate.verify(keyPair.getPublic());

        KeyStore keyStore = KeyStore.getInstance(TYPE);
        keyStore.load(null, password);
        keyStore.setKeyEntry(ALIAS, keyPair.getPrivate(), password,
                new java.security.cert.Certificate[] {certificate});
        return keyStore;
    }

    private static void restrictToOwner(Path path) {
        try {
            Files.setPosixFilePermissions(path, EnumSet.of(
                    PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE));
        } catch (UnsupportedOperationException | IOException ignored) {
            // Windows ACLs inherit from the user's folder; POSIX systems get explicit 0600.
        }
    }

    static final class LoadedKeystore {
        final KeyStore keyStore;
        final char[] password;

        LoadedKeystore(KeyStore keyStore, char[] password) {
            this.keyStore = keyStore;
            this.password = password;
        }
    }
}
