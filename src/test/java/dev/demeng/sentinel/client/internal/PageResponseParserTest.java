package dev.demeng.sentinel.client.internal;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.demeng.sentinel.client.license.License;
import dev.demeng.sentinel.client.license.Page;
import org.junit.jupiter.api.Test;

class PageResponseParserTest {

  private final PageResponseParser parser = new PageResponseParser(new LicenseResponseParser());

  private JsonObject resultWith(String pageJson) {
    return JsonParser.parseString("{\"page\":" + pageJson + "}").getAsJsonObject();
  }

  @Test
  void parsesPageWithOneLicense() {
    String json =
        """
        {
          "content": [{
            "id": "uuid-1", "key": "KEY-1",
            "product": {"id": "p1", "name": "Prod", "description": null, "logoUrl": null},
            "tier": null, "issuer": {"type": "API_KEY", "id": "i1", "displayName": "Admin"},
            "createdAt": "2026-01-01T00:00:00Z", "expiration": null,
            "maxServers": -1, "maxIps": -1, "note": null,
            "connections": {}, "subUsers": [], "servers": {}, "ips": {},
            "additionalEntitlements": [], "entitlements": [], "blacklist": null
          }],
          "page": {"size": 50, "number": 0, "totalElements": 1, "totalPages": 1}
        }
        """;

    Page<License> result = parser.parse(resultWith(json));

    assertEquals(1, result.content().size());
    assertEquals("KEY-1", result.content().get(0).key());
    assertEquals(50, result.size());
    assertEquals(0, result.number());
    assertEquals(1, result.totalElements());
    assertEquals(1, result.totalPages());
  }

  @Test
  void parsesEmptyPage() {
    String json =
        """
        {
          "content": [],
          "page": {"size": 50, "number": 0, "totalElements": 0, "totalPages": 0}
        }
        """;

    Page<License> result = parser.parse(resultWith(json));

    assertTrue(result.content().isEmpty());
    assertEquals(0, result.totalElements());
  }
}
