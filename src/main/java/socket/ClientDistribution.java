package socket;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.Base64;
import org.json.JSONObject;

/** Immutable signed client release exposed by this game server. */
final class ClientDistribution {
    static final long MAX_ARCHIVE_BYTES = 512L * 1024L * 1024L;
    static final int MAX_MANIFEST_BYTES = 256 * 1024;
    private static volatile ClientDistribution instance;

    private final Path manifestPath;
    private final Path archivePath;
    private final byte[] manifest;
    private final String version;
    private final String manifestSha256;

    private ClientDistribution() throws IOException {
        Path root = Paths.get(main.ServerConfig.getInstance().getClientDistributionDirectory())
                .toAbsolutePath().normalize();
        manifestPath = root.resolve("manifest.json");
        archivePath = root.resolve("client.zip");
        if (!Files.isRegularFile(manifestPath) || !Files.isRegularFile(archivePath)) {
            throw new IOException("Signed client distribution is missing from " + root);
        }
        long manifestSize = Files.size(manifestPath);
        long archiveSize = Files.size(archivePath);
        if (manifestSize <= 0 || manifestSize > MAX_MANIFEST_BYTES) {
            throw new IOException("Client manifest size is invalid");
        }
        if (archiveSize <= 0 || archiveSize > MAX_ARCHIVE_BYTES) {
            throw new IOException("Client archive size is invalid");
        }
        manifest = Files.readAllBytes(manifestPath);
        try {
            JSONObject envelope = new JSONObject(new String(manifest, StandardCharsets.UTF_8));
            byte[] payload = Base64.getDecoder().decode(envelope.getString("payload"));
            JSONObject release = new JSONObject(new String(payload, StandardCharsets.UTF_8));
            version = release.getString("clientVersion");
            if (!version.matches("[A-Za-z0-9._-]{1,64}")) throw new IOException("Invalid client version");
        } catch (Exception malformed) {
            throw new IOException("Client manifest is malformed", malformed);
        }
        manifestSha256 = sha256(manifest);
    }

    static ClientDistribution get() throws IOException {
        ClientDistribution value = instance;
        if (value == null) {
            synchronized (ClientDistribution.class) {
                value = instance;
                if (value == null) instance = value = new ClientDistribution();
            }
        }
        return value;
    }

    static ClientDistribution getIfAvailable() {
        try { return get(); }
        catch (Exception unavailable) { return null; }
    }

    String getVersion() { return version; }
    String getManifestSha256() { return manifestSha256; }
    byte[] getManifest() { return manifest.clone(); }
    Path getArchivePath() { return archivePath; }

    private static String sha256(byte[] bytes) throws IOException {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
            StringBuilder result = new StringBuilder(64);
            for (byte value : digest) result.append(String.format("%02x", value & 0xff));
            return result.toString();
        } catch (Exception impossible) {
            throw new IOException("SHA-256 is unavailable", impossible);
        }
    }
}
