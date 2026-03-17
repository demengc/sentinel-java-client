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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LicenseSubUserOperationsTest {

  private SentinelHttpClient mockHttp;
  private LicenseSubUserOperations ops;

  private static final String LICENSE_RESPONSE =
      """
      {
        "timestamp": 100, "status": "OK", "type": "SUCCESS", "message": "OK",
        "result": {"license": {
          "id": "uuid-1", "key": "KEY-123",
          "product": {"id": "p1", "name": "Prod", "description": null, "logoUrl": null},
          "tier": null, "issuer": {"type": "API_KEY", "id": "i1", "displayName": "Admin"},
          "createdAt": "2026-01-01T00:00:00Z", "expiration": null,
          "maxServers": -1, "maxIps": -1, "note": null,
          "connections": {}, "subUsers": [{"platform": "Discord", "value": "user123"}],
          "servers": {}, "ips": {},
          "additionalEntitlements": [], "entitlements": [], "blacklist": null
        }}
      }
      """;

  @BeforeEach
  void setUp() {
    mockHttp = mock(SentinelHttpClient.class);
    ops =
        new LicenseSubUserOperations(
            mockHttp, new ApiResponseParser(), new LicenseResponseParser(), new Gson());
  }

  @Test
  void addSubUsers() throws SentinelException {
    when(mockHttp.request(eq("POST"), eq("/api/v2/licenses/KEY-123/sub-users"), anyString()))
        .thenReturn(new SentinelHttpResponse(200, LICENSE_RESPONSE, Map.of()));

    License result = ops.add("KEY-123", List.of(new SubUser("Discord", "user123")));

    assertEquals(1, result.subUsers().size());
  }

  @Test
  void removeSubUsers() throws SentinelException {
    when(mockHttp.request(eq("POST"), eq("/api/v2/licenses/KEY-123/sub-users/remove"), anyString()))
        .thenReturn(new SentinelHttpResponse(200, LICENSE_RESPONSE, Map.of()));

    License result = ops.remove("KEY-123", List.of(new SubUser("Discord", "user123")));

    assertNotNull(result);
    verify(mockHttp)
        .request(eq("POST"), eq("/api/v2/licenses/KEY-123/sub-users/remove"), anyString());
  }
}
