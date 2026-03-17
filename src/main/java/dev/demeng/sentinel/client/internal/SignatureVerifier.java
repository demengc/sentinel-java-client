package dev.demeng.sentinel.client.internal;

import dev.demeng.sentinel.client.exception.SignatureVerificationException;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;

public final class SignatureVerifier {

  private final PublicKey publicKey;

  public SignatureVerifier(String base64PublicKey) {
    this.publicKey = parsePublicKey(base64PublicKey);
  }

  public static PublicKey parsePublicKey(String base64PublicKey) {
    try {
      byte[] keyBytes = Base64.getDecoder().decode(base64PublicKey);
      KeyFactory keyFactory = KeyFactory.getInstance("Ed25519");
      return keyFactory.generatePublic(new X509EncodedKeySpec(keyBytes));
    } catch (Exception e) {
      throw new IllegalArgumentException("Invalid Ed25519 public key", e);
    }
  }

  public void verify(
      @Nullable String signatureBase64,
      String nonce,
      long timestamp,
      @Nullable String expiration,
      int serverCount,
      int maxServers,
      int ipCount,
      int maxIps,
      @Nullable String tier,
      @Nullable List<String> entitlements)
      throws SignatureVerificationException {
    if (signatureBase64 == null) {
      throw new SignatureVerificationException(
          "Response signature is null but signature verification is enabled");
    }

    String canonical =
        buildCanonicalPayload(
            nonce,
            timestamp,
            expiration,
            serverCount,
            maxServers,
            ipCount,
            maxIps,
            tier,
            entitlements);

    try {
      Signature sig = Signature.getInstance("Ed25519");
      sig.initVerify(publicKey);
      sig.update(canonical.getBytes(StandardCharsets.UTF_8));
      byte[] signatureBytes = Base64.getDecoder().decode(signatureBase64);
      if (!sig.verify(signatureBytes)) {
        throw new SignatureVerificationException("Signature verification failed");
      }
    } catch (SignatureVerificationException e) {
      throw e;
    } catch (Exception e) {
      throw new SignatureVerificationException(
          "Signature verification error: " + e.getMessage(), e);
    }
  }

  public String buildCanonicalPayload(
      String nonce,
      long timestamp,
      @Nullable String expiration,
      int serverCount,
      int maxServers,
      int ipCount,
      int maxIps,
      @Nullable String tier,
      @Nullable List<String> entitlements) {
    StringBuilder sb = new StringBuilder();
    sb.append('{');
    sb.append("\"entitlements\":");
    if (entitlements == null) {
      sb.append("null");
    } else {
      sb.append('[');
      sb.append(
          entitlements.stream()
              .sorted()
              .map(SignatureVerifier::jsonString)
              .collect(Collectors.joining(",")));
      sb.append(']');
    }
    sb.append(",\"expiration\":").append(jsonString(expiration));
    sb.append(",\"ipCount\":").append(ipCount);
    sb.append(",\"maxIps\":").append(maxIps);
    sb.append(",\"maxServers\":").append(maxServers);
    sb.append(",\"nonce\":").append(jsonString(nonce));
    sb.append(",\"serverCount\":").append(serverCount);
    sb.append(",\"tier\":").append(jsonString(tier));
    sb.append(",\"timestamp\":").append(timestamp);
    sb.append('}');
    return sb.toString();
  }

  private static String jsonString(@Nullable String value) {
    if (value == null) return "null";
    StringBuilder sb = new StringBuilder("\"");
    for (int i = 0; i < value.length(); i++) {
      char c = value.charAt(i);
      switch (c) {
        case '"' -> sb.append("\\\"");
        case '\\' -> sb.append("\\\\");
        case '\n' -> sb.append("\\n");
        case '\r' -> sb.append("\\r");
        case '\t' -> sb.append("\\t");
        default -> {
          if (c < 0x20) {
            sb.append(String.format("\\u%04x", (int) c));
          } else {
            sb.append(c);
          }
        }
      }
    }
    return sb.append('"').toString();
  }
}
