package dev.demeng.sentinel.client.license;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CreateLicenseRequestTest {

  @Test
  void builderWithAllFields() {
    CreateLicenseRequest req =
        CreateLicenseRequest.builder()
            .product("prod-1")
            .key("CUSTOM-KEY")
            .tier("tier-1")
            .expiration(Instant.parse("2026-12-31T23:59:59Z"))
            .maxServers(3)
            .maxIps(5)
            .note("A note")
            .connections(Map.of("Discord", "user123"))
            .additionalEntitlements(Set.of("extra"))
            .build();

    assertEquals("prod-1", req.getProduct());
    assertEquals("CUSTOM-KEY", req.getKey());
    assertEquals("tier-1", req.getTier());
    assertEquals(3, req.getMaxServers());
  }

  @Test
  void builderWithOnlyRequiredFields() {
    CreateLicenseRequest req = CreateLicenseRequest.builder().product("prod-1").build();

    assertEquals("prod-1", req.getProduct());
    assertNull(req.getKey());
    assertNull(req.getTier());
  }

  @Test
  void missingProductThrows() {
    assertThrows(IllegalStateException.class, () -> CreateLicenseRequest.builder().build());
  }
}
