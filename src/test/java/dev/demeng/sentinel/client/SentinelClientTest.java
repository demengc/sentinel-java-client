package dev.demeng.sentinel.client;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import dev.demeng.sentinel.client.exception.*;
import dev.demeng.sentinel.client.internal.ReplayProtector;
import dev.demeng.sentinel.client.internal.SentinelHttpClient;
import dev.demeng.sentinel.client.internal.SentinelHttpResponse;
import dev.demeng.sentinel.client.internal.SignatureVerifier;
import dev.demeng.sentinel.client.license.validation.ValidationRequest;
import dev.demeng.sentinel.client.license.validation.ValidationResult;
import dev.demeng.sentinel.client.license.validation.ValidationResultType;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class SentinelClientTest {

  private static KeyPair keyPair;
  private static String publicKeyBase64;

  @BeforeAll
  static void generateKeyPair() throws Exception {
    KeyPairGenerator gen = KeyPairGenerator.getInstance("Ed25519");
    keyPair = gen.generateKeyPair();
    publicKeyBase64 = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
  }

  private static String sign(String payload) throws Exception {
    Signature sig = Signature.getInstance("Ed25519");
    sig.initSign(keyPair.getPrivate());
    sig.update(payload.getBytes(StandardCharsets.UTF_8));
    return Base64.getEncoder().encodeToString(sig.sign());
  }

  private SentinelClient clientWithMockedHttp(
      SentinelHttpResponse response, boolean withSignatureVerification)
      throws SentinelConnectionException {
    SentinelHttpClient mockHttp = mock(SentinelHttpClient.class);
    when(mockHttp.request(anyString(), anyString(), anyString())).thenReturn(response);

    SignatureVerifier verifier =
        withSignatureVerification ? new SignatureVerifier(publicKeyBase64) : null;
    ReplayProtector protector =
        withSignatureVerification ? new ReplayProtector(Duration.ofSeconds(30), 1000) : null;

    return new SentinelClient(mockHttp, verifier, protector);
  }

  private ValidationRequest testRequest() {
    return ValidationRequest.builder().product("test-product").server("test-server").build();
  }

  @Test
  void fullSuccessFlowWithoutSignatureVerification() throws SentinelException {
    String json =
        """
                {
                  "timestamp": 1741262400000, "status": "OK", "type": "SUCCESS",
                  "message": "License validated.",
                  "result": {"validation": {"nonce": "n1", "timestamp": 1741262400000,
                    "details": {"expiration": "2026-12-31T23:59:59Z", "serverCount": 1,
                      "maxServers": 3, "ipCount": 1, "maxIps": 3, "tier": "Pro",
                      "entitlements": ["a"]}, "signature": null}}
                }
                """;

    SentinelClient client =
        clientWithMockedHttp(new SentinelHttpResponse(200, json, Map.of()), false);
    ValidationResult result = client.licenses().validate(testRequest());

    assertTrue(result.isValid());
    assertEquals("Pro", result.getDetails().tier());
  }

  @Test
  void fullSuccessFlowWithSignatureVerification() throws Exception {
    long now = System.currentTimeMillis();
    String nonce = "test-nonce-123";

    SignatureVerifier tempVerifier = new SignatureVerifier(publicKeyBase64);
    String canonical =
        tempVerifier.buildCanonicalPayload(
            nonce, now, "2026-12-31T23:59:59Z", 1, 3, 1, 3, "Pro", List.of("feat-a"));
    String signature = sign(canonical);

    String json =
        String.format(
            """
                {
                  "timestamp": %d, "status": "OK", "type": "SUCCESS",
                  "message": "License validated.",
                  "result": {"validation": {"nonce": "%s", "timestamp": %d,
                    "details": {"expiration": "2026-12-31T23:59:59Z", "serverCount": 1,
                      "maxServers": 3, "ipCount": 1, "maxIps": 3, "tier": "Pro",
                      "entitlements": ["feat-a"]}, "signature": "%s"}}
                }
                """,
            now, nonce, now, signature);

    SentinelClient client =
        clientWithMockedHttp(new SentinelHttpResponse(200, json, Map.of()), true);
    ValidationResult result = client.licenses().validate(testRequest());

    assertTrue(result.isValid());
  }

  @Test
  void signatureVerificationFailsOnTamperedResponse() throws Exception {
    long now = System.currentTimeMillis();
    String nonce = "test-nonce-456";

    SignatureVerifier tempVerifier = new SignatureVerifier(publicKeyBase64);
    String canonical =
        tempVerifier.buildCanonicalPayload(
            nonce, now, "2026-12-31T23:59:59Z", 1, 3, 1, 3, "Pro", List.of());
    String signature = sign(canonical);

    String json =
        String.format(
            """
                {
                  "timestamp": %d, "status": "OK", "type": "SUCCESS",
                  "message": "License validated.",
                  "result": {"validation": {"nonce": "%s", "timestamp": %d,
                    "details": {"expiration": "2026-12-31T23:59:59Z", "serverCount": 1,
                      "maxServers": 3, "ipCount": 1, "maxIps": 3, "tier": "Basic",
                      "entitlements": []}, "signature": "%s"}}
                }
                """,
            now, nonce, now, signature);

    SentinelClient client =
        clientWithMockedHttp(new SentinelHttpResponse(200, json, Map.of()), true);

    assertThrows(
        SignatureVerificationException.class, () -> client.licenses().validate(testRequest()));
  }

  @Test
  void replayProtectionRejectsDuplicateNonce() throws Exception {
    long now = System.currentTimeMillis();
    String nonce = "duplicate-nonce";

    SignatureVerifier tempVerifier = new SignatureVerifier(publicKeyBase64);
    String canonical =
        tempVerifier.buildCanonicalPayload(nonce, now, null, 0, -1, 0, -1, null, null);
    String signature = sign(canonical);

    String json =
        String.format(
            """
                {
                  "timestamp": %d, "status": "OK", "type": "SUCCESS",
                  "message": "OK",
                  "result": {"validation": {"nonce": "%s", "timestamp": %d,
                    "details": {"expiration": null, "serverCount": 0,
                      "maxServers": -1, "ipCount": 0, "maxIps": -1, "tier": null,
                      "entitlements": null}, "signature": "%s"}}
                }
                """,
            now, nonce, now, signature);

    SentinelHttpResponse httpResponse = new SentinelHttpResponse(200, json, Map.of());
    SentinelHttpClient mockHttp = mock(SentinelHttpClient.class);
    when(mockHttp.request(anyString(), anyString(), anyString())).thenReturn(httpResponse);

    SignatureVerifier verifier = new SignatureVerifier(publicKeyBase64);
    ReplayProtector protector = new ReplayProtector(Duration.ofSeconds(30), 1000);
    SentinelClient client = new SentinelClient(mockHttp, verifier, protector);

    client.licenses().validate(testRequest());
    assertThrows(ReplayDetectedException.class, () -> client.licenses().validate(testRequest()));
  }

  @Test
  void validationFailureReturnsResultNotException() throws SentinelException {
    String json =
        """
                {"timestamp": 0, "status": "FORBIDDEN", "type": "EXPIRED_LICENSE",
                 "message": "License has expired.", "result": {}}
                """;

    SentinelClient client =
        clientWithMockedHttp(new SentinelHttpResponse(403, json, Map.of()), false);
    ValidationResult result = client.licenses().validate(testRequest());

    assertFalse(result.isValid());
    assertEquals(ValidationResultType.EXPIRED_LICENSE, result.getType());
  }

  @Test
  void buildWithInvalidPublicKeyThrows() {
    IllegalStateException ex =
        assertThrows(
            IllegalStateException.class,
            () ->
                SentinelClient.builder()
                    .baseUrl("https://api.example.com")
                    .apiKey("test-key")
                    .publicKey("not-a-valid-key!!!")
                    .build());
    assertTrue(ex.getMessage().contains("publicKey"));
  }

  @Test
  void apiErrorThrowsException() throws SentinelConnectionException {
    String json =
        """
                {"timestamp": 0, "status": "UNAUTHORIZED", "type": "UNAUTHORIZED",
                 "message": "Invalid API key.", "result": null}
                """;

    SentinelClient client =
        clientWithMockedHttp(new SentinelHttpResponse(401, json, Map.of()), false);

    SentinelApiException ex =
        assertThrows(SentinelApiException.class, () -> client.licenses().validate(testRequest()));
    assertEquals(401, ex.getHttpStatus());
  }
}
