package dev.demeng.sentinel.client.internal;

import static org.junit.jupiter.api.Assertions.*;

import dev.demeng.sentinel.client.exception.SignatureVerificationException;
import java.security.*;
import java.util.Base64;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class SignatureVerifierTest {

  private static KeyPair keyPair;
  private static SignatureVerifier verifier;

  @BeforeAll
  static void generateKeyPair() throws Exception {
    KeyPairGenerator gen = KeyPairGenerator.getInstance("Ed25519");
    keyPair = gen.generateKeyPair();
    String publicKeyBase64 = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
    verifier = new SignatureVerifier(publicKeyBase64);
  }

  private String sign(String payload) throws Exception {
    Signature sig = Signature.getInstance("Ed25519");
    sig.initSign(keyPair.getPrivate());
    sig.update(payload.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    return Base64.getEncoder().encodeToString(sig.sign());
  }

  @Test
  void verifiesValidSignature() throws Exception {
    String nonce = "test-nonce";
    long timestamp = 1741262400000L;
    String expiration = "2026-12-31T23:59:59Z";
    int serverCount = 1;
    int maxServers = 3;
    int ipCount = 1;
    int maxIps = 3;
    String tier = "Pro";
    List<String> entitlements = List.of("advanced-features", "priority-support");

    String canonical =
        verifier.buildCanonicalPayload(
            nonce,
            timestamp,
            expiration,
            serverCount,
            maxServers,
            ipCount,
            maxIps,
            tier,
            entitlements);
    String signature = sign(canonical);

    assertDoesNotThrow(
        () ->
            verifier.verify(
                signature,
                nonce,
                timestamp,
                expiration,
                serverCount,
                maxServers,
                ipCount,
                maxIps,
                tier,
                entitlements));
  }

  @Test
  void rejectsTamperedSignature() throws Exception {
    String canonical =
        verifier.buildCanonicalPayload(
            "nonce", 1000L, "2026-12-31T23:59:59Z", 1, 3, 1, 3, "Pro", List.of());
    String signature = sign(canonical);

    assertThrows(
        SignatureVerificationException.class,
        () ->
            verifier.verify(
                signature, "nonce", 1000L, "2026-12-31T23:59:59Z", 1, 3, 1, 3, "Basic", List.of()));
  }

  @Test
  void rejectsNullSignatureWhenVerificationExpected() {
    assertThrows(
        SignatureVerificationException.class,
        () -> verifier.verify(null, "nonce", 1000L, null, 1, 3, 1, 3, null, null));
  }

  @Test
  void canonicalPayloadSortsEntitlements() {
    String payload =
        verifier.buildCanonicalPayload(
            "n", 1000L, null, 1, 3, 1, 3, null, List.of("zebra", "alpha"));
    assertTrue(payload.contains("\"alpha\",\"zebra\""));
  }

  @Test
  void canonicalPayloadHandlesNulls() {
    String payload = verifier.buildCanonicalPayload("n", 1000L, null, 1, 3, 1, 3, null, null);
    assertTrue(payload.contains("\"entitlements\":null"));
    assertTrue(payload.contains("\"expiration\":null"));
    assertTrue(payload.contains("\"tier\":null"));
  }
}
