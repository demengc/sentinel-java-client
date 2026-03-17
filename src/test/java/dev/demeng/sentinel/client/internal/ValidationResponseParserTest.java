package dev.demeng.sentinel.client.internal;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.demeng.sentinel.client.exception.SentinelApiException;
import dev.demeng.sentinel.client.license.validation.FailureDetails;
import dev.demeng.sentinel.client.license.validation.ValidationDetails;
import dev.demeng.sentinel.client.license.validation.ValidationResult;
import dev.demeng.sentinel.client.license.validation.ValidationResultType;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class ValidationResponseParserTest {

  private final ValidationResponseParser parser = new ValidationResponseParser();

  private ApiResponseParser.ApiResponse apiResponse(
      int status, String type, String message, String resultJson) {
    JsonObject result =
        resultJson != null ? JsonParser.parseString(resultJson).getAsJsonObject() : null;
    return new ApiResponseParser.ApiResponse(status, type, message, result);
  }

  @Test
  void parses200SuccessResponse() throws SentinelApiException {
    String resultJson =
        """
                {
                  "validation": {
                    "nonce": "b3a1d9f4-7e2c-4a8b-9c6d-5e0f1a2b3c4d",
                    "timestamp": 1741262400000,
                    "details": {
                      "expiration": "2026-12-31T23:59:59Z",
                      "serverCount": 1,
                      "maxServers": 3,
                      "ipCount": 1,
                      "maxIps": 3,
                      "tier": "Pro",
                      "entitlements": ["advanced-features", "priority-support"]
                    },
                    "signature": "dGVzdC1zaWduYXR1cmU="
                  }
                }
                """;

    var parsed = parser.parse(apiResponse(200, "SUCCESS", "License validated.", resultJson));
    ValidationResult result = parsed.result();
    assertTrue(result.isValid());
    assertEquals("License validated.", result.getMessage());

    ValidationDetails details = result.getDetails();
    assertEquals(Instant.parse("2026-12-31T23:59:59Z"), details.expiration());
    assertEquals(1, details.serverCount());
    assertEquals(3, details.maxServers());
    assertEquals(1, details.ipCount());
    assertEquals(3, details.maxIps());
    assertEquals("Pro", details.tier());
    assertTrue(details.entitlements().contains("advanced-features"));
    assertTrue(details.entitlements().contains("priority-support"));

    assertEquals("b3a1d9f4-7e2c-4a8b-9c6d-5e0f1a2b3c4d", parsed.nonce());
    assertEquals(1741262400000L, parsed.timestamp());
    assertEquals("dGVzdC1zaWduYXR1cmU=", parsed.signature());
    assertEquals("2026-12-31T23:59:59Z", parsed.expiration());
    assertEquals(1, parsed.serverCount());
    assertEquals(3, parsed.maxServers());
    assertEquals(1, parsed.ipCount());
    assertEquals(3, parsed.maxIps());
    assertEquals("Pro", parsed.tier());
    assertEquals(List.of("advanced-features", "priority-support"), parsed.entitlements());
  }

  @Test
  void parses200WithNullOptionalFields() throws SentinelApiException {
    String resultJson =
        """
                {
                  "validation": {
                    "nonce": "abc",
                    "timestamp": 1741262400000,
                    "details": {
                      "expiration": null,
                      "serverCount": 1,
                      "maxServers": -1,
                      "ipCount": 0,
                      "maxIps": -1,
                      "tier": "Default",
                      "entitlements": null
                    },
                    "signature": null
                  }
                }
                """;

    var parsed = parser.parse(apiResponse(200, "SUCCESS", "License validated.", resultJson));
    ValidationDetails details = parsed.result().getDetails();
    assertNull(details.expiration());
    assertEquals("Default", details.tier());
    assertTrue(details.entitlements().isEmpty());
    assertNull(parsed.signature());
    assertNull(parsed.expiration());
    assertEquals("Default", parsed.tier());
    assertNull(parsed.entitlements());
  }

  @Test
  void parses403ValidationFailure() throws SentinelApiException {
    var parsed = parser.parse(apiResponse(403, "INVALID_LICENSE", "License key is invalid.", "{}"));
    ValidationResult result = parsed.result();
    assertFalse(result.isValid());
    assertEquals(ValidationResultType.INVALID_LICENSE, result.getType());
    assertEquals("License key is invalid.", result.getMessage());
    assertNull(result.getFailureDetails());
  }

  @Test
  void parses403BlacklistedWithDetails() throws SentinelApiException {
    String resultJson =
        """
                {"blacklist": {"timestamp": "2026-01-15T10:30:00Z", "reason": "Chargeback"}}
                """;
    var parsed =
        parser.parse(
            apiResponse(403, "BLACKLISTED_LICENSE", "License is blacklisted.", resultJson));
    ValidationResult result = parsed.result();
    assertFalse(result.isValid());
    assertEquals(ValidationResultType.BLACKLISTED_LICENSE, result.getType());

    assertInstanceOf(FailureDetails.BlacklistDetails.class, result.getFailureDetails());
    var details = (FailureDetails.BlacklistDetails) result.getFailureDetails();
    assertEquals(Instant.parse("2026-01-15T10:30:00Z"), details.timestamp());
    assertEquals("Chargeback", details.reason());
  }

  @Test
  void parses403ExcessiveServersWithDetails() throws SentinelApiException {
    String resultJson =
        """
                {"maxServers": 3}
                """;
    var parsed =
        parser.parse(apiResponse(403, "EXCESSIVE_SERVERS", "Too many servers.", resultJson));
    assertInstanceOf(
        FailureDetails.ExcessiveServersDetails.class, parsed.result().getFailureDetails());
    assertEquals(
        3,
        ((FailureDetails.ExcessiveServersDetails) parsed.result().getFailureDetails())
            .maxServers());
  }

  @Test
  void parses403ExcessiveIpsWithDetails() throws SentinelApiException {
    String resultJson =
        """
                {"maxIps": 5}
                """;
    var parsed = parser.parse(apiResponse(403, "EXCESSIVE_IPS", "Too many IPs.", resultJson));
    assertInstanceOf(FailureDetails.ExcessiveIpsDetails.class, parsed.result().getFailureDetails());
    assertEquals(
        5, ((FailureDetails.ExcessiveIpsDetails) parsed.result().getFailureDetails()).maxIps());
  }

  @Test
  void parses403FailureDetailsGracefullyDegradeOnMalformedData() throws SentinelApiException {
    var parsed =
        parser.parse(
            apiResponse(403, "BLACKLISTED_LICENSE", "Blacklisted.", "{\"unrelated\": true}"));
    assertFalse(parsed.result().isValid());
    assertNull(parsed.result().getFailureDetails());
  }

  @Test
  void throws403UnknownType() {
    SentinelApiException ex =
        assertThrows(
            SentinelApiException.class,
            () -> parser.parse(apiResponse(403, "NEW_TYPE", "msg", "{}")));
    assertEquals(403, ex.getHttpStatus());
    assertEquals("NEW_TYPE", ex.getType());
  }

  @Test
  void throwsOnUnexpectedStatusCode() {
    SentinelApiException ex =
        assertThrows(
            SentinelApiException.class,
            () -> parser.parse(apiResponse(201, "SUCCESS", "Created.", "{}")));
    assertEquals(201, ex.getHttpStatus());
  }

  @Test
  void throwsOnMalformedSuccessResult() {
    assertThrows(
        SentinelApiException.class,
        () -> parser.parse(apiResponse(200, "SUCCESS", "OK", "{\"notValidation\": true}")));
  }
}
