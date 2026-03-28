package dev.demeng.sentinel.client.license;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.google.gson.Gson;
import dev.demeng.sentinel.client.exception.SentinelException;
import dev.demeng.sentinel.client.internal.ApiResponseParser;
import dev.demeng.sentinel.client.internal.LicenseResponseParser;
import dev.demeng.sentinel.client.internal.SentinelHttpClient;
import dev.demeng.sentinel.client.internal.SentinelHttpResponse;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LicenseIpOperationsTest {

  private SentinelHttpClient mockHttp;
  private LicenseIpOperations ops;

  private static final String LICENSE_RESPONSE =
      """
      {
        "timestamp": 100, "status": "OK", "type": "SUCCESS", "message": "OK",
        "result": {"license": {
          "id": "uuid-1", "key": "KEY-123",
          "product": {"id": "p1", "name": "Prod", "description": null, "logoUrl": null},
          "tier": {"name": "Default", "entitlements": []}, "issuer": {"type": "API_KEY", "id": "i1", "displayName": "Admin"},
          "createdAt": "2026-01-01T00:00:00Z", "expiration": null,
          "maxServers": -1, "maxIps": -1, "note": null,
          "connections": {}, "subUsers": [], "servers": {},
          "ips": {"1.2.3.4": "2026-01-15T10:00:00Z"},
          "additionalEntitlements": [], "entitlements": [], "blacklist": null
        }}
      }
      """;

  @BeforeEach
  void setUp() {
    mockHttp = mock(SentinelHttpClient.class);
    ops =
        new LicenseIpOperations(
            mockHttp, new ApiResponseParser(), new LicenseResponseParser(), new Gson());
  }

  @Test
  void addIps() throws SentinelException {
    when(mockHttp.request(eq("POST"), eq("/api/v2/licenses/KEY-123/ips"), anyString()))
        .thenReturn(new SentinelHttpResponse(200, LICENSE_RESPONSE, Map.of()));

    License result = ops.add("KEY-123", Set.of("1.2.3.4"));

    assertEquals(1, result.ips().size());
  }

  @Test
  void removeIps() throws SentinelException {
    when(mockHttp.requestWithMultiValuedParams(
            eq("DELETE"), eq("/api/v2/licenses/KEY-123/ips"), anyMap()))
        .thenReturn(new SentinelHttpResponse(200, LICENSE_RESPONSE, Map.of()));

    License result = ops.remove("KEY-123", Set.of("1.2.3.4"));

    assertNotNull(result);
    verify(mockHttp)
        .requestWithMultiValuedParams(
            eq("DELETE"),
            eq("/api/v2/licenses/KEY-123/ips"),
            eq(Map.of("ips", List.of("1.2.3.4"))));
  }
}
