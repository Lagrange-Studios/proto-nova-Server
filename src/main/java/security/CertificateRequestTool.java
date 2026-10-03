package security;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.EnumSet;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.ExtensionsGenerator;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;

/** Creates a signing request for website admin review while retaining the server's private key. */
public final class CertificateRequestTool {
  private CertificateRequestTool() {}

  public static Path create(String hostname, Path directory) throws Exception {
    if (!hostname.equals(hostname.toLowerCase(java.util.Locale.ROOT))
        || hostname.length() > 253
        || !hostname.matches(
            "[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?(?:\\.[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?)+")) {
      throw new IllegalArgumentException(
          "Enter a lowercase DNS hostname without a port or wildcard.");
    }
    Files.createDirectories(directory);
    protect(directory, true);
    Path keyPath = directory.resolve("privkey.pem");
    PrivateKey key;
    if (Files.exists(keyPath)) {
      try (var reader = Files.newBufferedReader(keyPath);
          var parser = new PEMParser(reader)) {
        Object pem = parser.readObject();
        var converter = new JcaPEMKeyConverter();
        if (pem instanceof PrivateKeyInfo info) key = converter.getPrivateKey(info);
        else if (pem instanceof org.bouncycastle.openssl.PEMKeyPair pair)
          key = converter.getPrivateKey(pair.getPrivateKeyInfo());
        else
          throw new IllegalArgumentException(
              "Existing request key must be an unencrypted PEM private key.");
      }
    } else {
      KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
      generator.initialize(3072);
      key = generator.generateKeyPair().getPrivate();
      try (var writer =
          new JcaPEMWriter(
              Files.newBufferedWriter(
                  keyPath,
                  java.nio.file.StandardOpenOption.CREATE_NEW,
                  java.nio.file.StandardOpenOption.WRITE))) {
        writer.writeObject(
            new org.bouncycastle.util.io.pem.PemObject("PRIVATE KEY", key.getEncoded()));
      }
    }
    protect(keyPath, false);
    if (!(key instanceof RSAPrivateCrtKey rsa))
      throw new IllegalArgumentException("Certificate requests require an RSA key.");
    var publicKey =
        java.security.KeyFactory.getInstance("RSA")
            .generatePublic(new RSAPublicKeySpec(rsa.getModulus(), rsa.getPublicExponent()));
    var extensions = new ExtensionsGenerator();
    extensions.addExtension(
        Extension.subjectAlternativeName,
        false,
        new GeneralNames(new GeneralName(GeneralName.dNSName, hostname)));
    var builder =
        new JcaPKCS10CertificationRequestBuilder(new X500Name("CN=" + hostname), publicKey);
    builder.addAttribute(PKCSObjectIdentifiers.pkcs_9_at_extensionRequest, extensions.generate());
    var request = builder.build(new JcaContentSignerBuilder("SHA256withRSA").build(key));
    Path requestPath = directory.resolve("server-request.csr");
    try (var writer = new JcaPEMWriter(Files.newBufferedWriter(requestPath))) {
      writer.writeObject(request);
    }
    return requestPath;
  }

  private static void protect(Path path, boolean directory) throws java.io.IOException {
    try {
      var permissions = EnumSet.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE);
      if (directory) permissions.add(PosixFilePermission.OWNER_EXECUTE);
      Files.setPosixFilePermissions(path, permissions);
    } catch (UnsupportedOperationException ignored) {
      // Windows inherits the hosting account's directory ACLs.
    }
  }
}
