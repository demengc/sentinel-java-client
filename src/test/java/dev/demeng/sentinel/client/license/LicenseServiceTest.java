package dev.demeng.sentinel.client.license;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.google.gson.Gson;
import dev.demeng.sentinel.client.exception.SentinelApiException;
import dev.demeng.sentinel.client.exception.SentinelException;
import dev.demeng.sentinel.client.internal.ApiResponseParser;
import dev.demeng.sentinel.client.internal.LicenseResponseParser;
import dev.demeng.sentinel.client.internal.PageResponseParser;
import dev.demeng.sentinel.client.internal.SentinelHttpClient;
import dev.demeng.sentinel.client.internal.SentinelHttpResponse;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LicenseServiceTest {

  private SentinelHttpClient mockHttp;
  private LicenseService service;

  @BeforeEach
  void setUp() {
    mockHttp = mock(SentinelHttpClient.class);
    Gson gson = new Gson();
    ApiResponseParser apiParser = new ApiResponseParser();
    LicenseResponseParser licenseParser = new LicenseResponseParser();
    PageResponseParser pageParser = new PageResponseParser(licenseParser);
    service = new LicenseService(mockHttp, apiParser, licenseParser, pageParser, gson, null, null);
  }

  private static final String LICENSE_RESPONSE =
      """
      {
        "timestamp": 100, "status": "OK", "type": "SUCCESS", "message": "OK",
        "result": {"license": {
          "id": "uuid-1", "key": "KEY-123",
          "product": {"id": "p1", "name": "Prod", "description": null, "logoUrl": null},
          "tier": {"id": "t1", "name": "Default", "entitlements": []}, "issuer": {"type": "API_KEY", "id": "i1", "displayName": "Admin"},
          "createdAt": "2026-01-01T00:00:00Z", "expiration": null,
          "maxServers": -1, "maxIps": -1, "note": null,
          "connections": {}, "subUsers": [], "servers": {}, "ips": {},
          "additionalEntitlements": [], "entitlements": [], "blacklist": null
        }}
      }
      """;

  @Test
  void createLicense() throws SentinelException {
    when(mockHttp.request(eq("POST"), eq("/api/v2/licenses"), anyString()))
        .thenReturn(new SentinelHttpResponse(200, LICENSE_RESPONSE, Map.of()));

    License license = service.create(CreateLicenseRequest.builder().product("Prod").build());

    assertEquals("KEY-123", license.key());
    verify(mockHttp).request(eq("POST"), eq("/api/v2/licenses"), anyString());
  }

  @Test
  void getLicense() throws SentinelException {
    when(mockHttp.request("GET", "/api/v2/licenses/KEY-123"))
        .thenReturn(new SentinelHttpResponse(200, LICENSE_RESPONSE, Map.of()));

    License license = service.get("KEY-123");

    assertEquals("KEY-123", license.key());
  }

  @Test
  void deleteLicenseHandles204() throws SentinelException {
    when(mockHttp.request("DELETE", "/api/v2/licenses/KEY-123"))
        .thenReturn(new SentinelHttpResponse(204, null, Map.of()));

    assertDoesNotThrow(() -> service.delete("KEY-123"));
  }

  @Test
  void deleteNon204ThrowsException() throws Exception {
    String errorJson =
        """
        {"timestamp": 100, "status": "NOT_FOUND", "type": "NOT_FOUND", "message": "Not found", "result": null}
        """;
    when(mockHttp.request("DELETE", "/api/v2/licenses/KEY-999"))
        .thenReturn(new SentinelHttpResponse(404, errorJson, Map.of()));

    assertThrows(SentinelApiException.class, () -> service.delete("KEY-999"));
  }

  @Test
  void listLicenses() throws SentinelException {
    String pageJson =
        """
        {
          "timestamp": 100, "status": "OK", "type": "SUCCESS", "message": "OK",
          "result": {"page": {
            "content": [{
              "id": "uuid-1", "key": "KEY-1",
              "product": {"id": "p1", "name": "Prod", "description": null, "logoUrl": null},
              "tier": {"id": "t1", "name": "Default", "entitlements": []}, "issuer": {"type": "API_KEY", "id": "i1", "displayName": "Admin"},
              "createdAt": "2026-01-01T00:00:00Z", "expiration": null,
              "maxServers": -1, "maxIps": -1, "note": null,
              "connections": {}, "subUsers": [], "servers": {}, "ips": {},
              "additionalEntitlements": [], "entitlements": [], "blacklist": null
            }],
            "page": {"size": 50, "number": 0, "totalElements": 1, "totalPages": 1}
          }}
        }
        """;
    when(mockHttp.request(eq("GET"), eq("/api/v2/licenses"), isNull(), anyMap()))
        .thenReturn(new SentinelHttpResponse(200, pageJson, Map.of()));

    Page<License> page = service.list(ListLicensesRequest.builder().build());

    assertEquals(1, page.content().size());
    assertEquals("KEY-1", page.content().get(0).key());
  }

  @Test
  void updateLicense() throws SentinelException {
    when(mockHttp.request(eq("PATCH"), eq("/api/v2/licenses/KEY-123"), anyString()))
        .thenReturn(new SentinelHttpResponse(200, LICENSE_RESPONSE, Map.of()));

    License license =
        service.update("KEY-123", UpdateLicenseRequest.builder().note("new note").build());

    assertEquals("KEY-123", license.key());
    verify(mockHttp).request(eq("PATCH"), eq("/api/v2/licenses/KEY-123"), anyString());
  }

  @Test
  void regenerateKeyWithNewKey() throws SentinelException {
    when(mockHttp.request(
            eq("POST"), eq("/api/v2/licenses/KEY-123/regenerate-key"), isNull(), anyMap()))
        .thenReturn(new SentinelHttpResponse(200, LICENSE_RESPONSE, Map.of()));

    License license = service.regenerateKey("KEY-123", "NEW-KEY");

    assertNotNull(license);
    verify(mockHttp)
        .request(
            eq("POST"),
            eq("/api/v2/licenses/KEY-123/regenerate-key"),
            isNull(),
            eq(Map.of("newKey", "NEW-KEY")));
  }

  @Test
  void regenerateKeyWithoutNewKey() throws SentinelException {
    when(mockHttp.request(
            eq("POST"), eq("/api/v2/licenses/KEY-123/regenerate-key"), isNull(), anyMap()))
        .thenReturn(new SentinelHttpResponse(200, LICENSE_RESPONSE, Map.of()));

    License license = service.regenerateKey("KEY-123");

    assertNotNull(license);
  }
}
