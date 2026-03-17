package dev.demeng.sentinel.client.license;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class UpdateLicenseRequestTest {

  @Test
  void onlySetFieldsArePresent() {
    UpdateLicenseRequest req = UpdateLicenseRequest.builder().note("updated note").build();

    assertTrue(req.isSet("note"));
    assertFalse(req.isSet("product"));
    assertFalse(req.isSet("expiration"));
    assertEquals("updated note", req.getNote());
  }

  @Test
  void clearExpirationSetsSentinelValue() {
    UpdateLicenseRequest req = UpdateLicenseRequest.builder().clearExpiration().build();

    assertTrue(req.isSet("expiration"));
    assertEquals(Instant.EPOCH, req.getExpiration());
  }

  @Test
  void settingExpirationExplicitly() {
    Instant exp = Instant.parse("2026-12-31T23:59:59Z");
    UpdateLicenseRequest req = UpdateLicenseRequest.builder().expiration(exp).build();

    assertTrue(req.isSet("expiration"));
    assertEquals(exp, req.getExpiration());
  }

  @Test
  void emptyBuilderProducesNoSetFields() {
    UpdateLicenseRequest req = UpdateLicenseRequest.builder().build();
    assertFalse(req.isSet("product"));
    assertFalse(req.isSet("note"));
    assertFalse(req.isSet("expiration"));
  }
}
