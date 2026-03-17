package dev.demeng.sentinel.client.internal;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.demeng.sentinel.client.license.License;
import org.junit.jupiter.api.Test;

class LicenseResponseParserTest {

  private final LicenseResponseParser parser = new LicenseResponseParser();

  private JsonObject resultWithLicense(String licenseJson) {
    return JsonParser.parseString("{\"license\":" + licenseJson + "}").getAsJsonObject();
  }

  @Test
  void parsesFullLicense() {
    String json =
        """
        {
          "id": "uuid-1",
          "key": "KEY-123",
          "product": {"id": "p1", "name": "MyProduct", "description": "A product", "logoUrl": "https://img.png"},
          "tier": {"id": "t1", "name": "Pro", "entitlements": ["feature-a"]},
          "issuer": {"type": "API_KEY", "id": "iss-1", "displayName": "Admin"},
          "createdAt": "2026-01-01T00:00:00Z",
          "expiration": "2026-12-31T23:59:59Z",
          "maxServers": 3,
          "maxIps": 5,
          "note": "Test note",
          "connections": {"Discord": "user123"},
          "subUsers": [{"platform": "Discord", "value": "user456"}],
          "servers": {"srv-1": "2026-01-15T10:00:00Z"},
          "ips": {"1.2.3.4": "2026-01-15T10:00:00Z"},
          "additionalEntitlements": ["extra-1"],
          "entitlements": ["feature-a", "extra-1"],
          "blacklist": null
        }
        """;

    License license = parser.parse(resultWithLicense(json));

    assertEquals("uuid-1", license.id());
    assertEquals("KEY-123", license.key());
    assertEquals("MyProduct", license.product().name());
    assertEquals("Pro", license.tier().name());
    assertEquals("API_KEY", license.issuer().type());
    assertEquals(3, license.maxServers());
    assertEquals(5, license.maxIps());
    assertEquals("Test note", license.note());
    assertEquals(1, license.connections().size());
    assertEquals("user123", license.connections().get("Discord"));
    assertEquals(1, license.subUsers().size());
    assertEquals(1, license.servers().size());
    assertEquals(1, license.ips().size());
    assertEquals(2, license.entitlements().size());
    assertNull(license.blacklist());
  }

  @Test
  void parsesLicenseWithNullOptionalFields() {
    String json =
        """
        {
          "id": "uuid-2",
          "key": "KEY-456",
          "product": {"id": "p1", "name": "Prod", "description": null, "logoUrl": null},
          "tier": {"id": "t1", "name": "Default", "entitlements": []},
          "issuer": {"type": "API_KEY", "id": "iss-1", "displayName": "Admin"},
          "createdAt": "2026-01-01T00:00:00Z",
          "expiration": null,
          "maxServers": -1,
          "maxIps": -1,
          "note": null,
          "connections": {},
          "subUsers": [],
          "servers": {},
          "ips": {},
          "additionalEntitlements": [],
          "entitlements": [],
          "blacklist": null
        }
        """;

    License license = parser.parse(resultWithLicense(json));

    assertEquals("Default", license.tier().name());
    assertNull(license.expiration());
    assertNull(license.note());
    assertEquals(-1, license.maxServers());
    assertTrue(license.connections().isEmpty());
    assertTrue(license.subUsers().isEmpty());
    assertTrue(license.servers().isEmpty());
    assertNull(license.blacklist());
  }

  @Test
  void parsesLicenseWithBlacklist() {
    String json =
        """
        {
          "id": "uuid-3",
          "key": "KEY-789",
          "product": {"id": "p1", "name": "Prod", "description": null, "logoUrl": null},
          "tier": {"id": "t1", "name": "Default", "entitlements": []},
          "issuer": {"type": "API_KEY", "id": "iss-1", "displayName": "Admin"},
          "createdAt": "2026-01-01T00:00:00Z",
          "expiration": null,
          "maxServers": -1,
          "maxIps": -1,
          "note": null,
          "connections": {},
          "subUsers": [],
          "servers": {},
          "ips": {},
          "additionalEntitlements": [],
          "entitlements": [],
          "blacklist": {"timestamp": "2026-02-01T00:00:00Z", "reason": "Abuse"}
        }
        """;

    License license = parser.parse(resultWithLicense(json));

    assertNotNull(license.blacklist());
    assertEquals("Abuse", license.blacklist().reason());
  }

  @Test
  void collectionsAreDefensiveCopies() {
    String json =
        """
        {
          "id": "uuid-4", "key": "K", "product": {"id": "p", "name": "P", "description": null, "logoUrl": null},
          "tier": {"id": "t1", "name": "Default", "entitlements": []}, "issuer": {"type": "T", "id": "I", "displayName": "D"},
          "createdAt": "2026-01-01T00:00:00Z", "expiration": null, "maxServers": -1, "maxIps": -1,
          "note": null, "connections": {"a": "b"}, "subUsers": [], "servers": {},
          "ips": {}, "additionalEntitlements": [], "entitlements": ["x"], "blacklist": null
        }
        """;
    License license = parser.parse(resultWithLicense(json));

    assertThrows(UnsupportedOperationException.class, () -> license.connections().put("c", "d"));
    assertThrows(UnsupportedOperationException.class, () -> license.entitlements().add("y"));
  }
}
