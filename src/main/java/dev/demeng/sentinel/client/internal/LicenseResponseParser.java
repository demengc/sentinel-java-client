package dev.demeng.sentinel.client.internal;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.demeng.sentinel.client.license.BlacklistInfo;
import dev.demeng.sentinel.client.license.License;
import dev.demeng.sentinel.client.license.LicenseIssuer;
import dev.demeng.sentinel.client.license.LicenseProduct;
import dev.demeng.sentinel.client.license.LicenseTier;
import dev.demeng.sentinel.client.license.SubUser;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jspecify.annotations.Nullable;

public final class LicenseResponseParser {

  public License parse(JsonObject result) {
    JsonObject obj = result.getAsJsonObject("license");
    return parseLicenseObject(obj);
  }

  License parseLicenseObject(JsonObject obj) {
    String id = obj.get("id").getAsString();
    String key = obj.get("key").getAsString();

    JsonObject productObj = obj.getAsJsonObject("product");
    LicenseProduct product =
        new LicenseProduct(
            productObj.get("id").getAsString(),
            productObj.get("name").getAsString(),
            getStringOrNull(productObj, "description"),
            getStringOrNull(productObj, "logoUrl"));

    LicenseTier tier = null;
    JsonElement tierEl = obj.get("tier");
    if (tierEl != null && !tierEl.isJsonNull()) {
      JsonObject tierObj = tierEl.getAsJsonObject();
      Set<String> tierEntitlements = new LinkedHashSet<>();
      JsonElement entEl = tierObj.get("entitlements");
      if (entEl != null && !entEl.isJsonNull()) {
        for (JsonElement e : entEl.getAsJsonArray()) {
          tierEntitlements.add(e.getAsString());
        }
      }
      tier =
          new LicenseTier(
              tierObj.get("id").getAsString(), getStringOrNull(tierObj, "name"), tierEntitlements);
    }

    JsonObject issuerObj = obj.getAsJsonObject("issuer");
    LicenseIssuer issuer =
        new LicenseIssuer(
            issuerObj.get("type").getAsString(),
            issuerObj.get("id").getAsString(),
            issuerObj.get("displayName").getAsString());

    Instant createdAt = Instant.parse(obj.get("createdAt").getAsString());
    Instant expiration = parseInstantOrNull(obj, "expiration");
    int maxServers = obj.get("maxServers").getAsInt();
    int maxIps = obj.get("maxIps").getAsInt();
    String note = getStringOrNull(obj, "note");

    Map<String, String> connections = new LinkedHashMap<>();
    JsonElement connEl = obj.get("connections");
    if (connEl != null && !connEl.isJsonNull()) {
      for (Map.Entry<String, JsonElement> entry : connEl.getAsJsonObject().entrySet()) {
        connections.put(entry.getKey(), entry.getValue().getAsString());
      }
    }

    List<SubUser> subUsers = new ArrayList<>();
    JsonElement subEl = obj.get("subUsers");
    if (subEl != null && !subEl.isJsonNull()) {
      for (JsonElement e : subEl.getAsJsonArray()) {
        JsonObject s = e.getAsJsonObject();
        subUsers.add(new SubUser(s.get("platform").getAsString(), s.get("value").getAsString()));
      }
    }

    Map<String, Instant> servers = parseInstantMap(obj, "servers");
    Map<String, Instant> ips = parseInstantMap(obj, "ips");

    Set<String> additionalEntitlements = parseStringSet(obj, "additionalEntitlements");
    Set<String> entitlements = parseStringSet(obj, "entitlements");

    BlacklistInfo blacklist = null;
    JsonElement blEl = obj.get("blacklist");
    if (blEl != null && !blEl.isJsonNull()) {
      JsonObject blObj = blEl.getAsJsonObject();
      blacklist =
          new BlacklistInfo(
              Instant.parse(blObj.get("timestamp").getAsString()),
              getStringOrNull(blObj, "reason"));
    }

    return new License(
        id,
        key,
        product,
        tier,
        issuer,
        createdAt,
        expiration,
        maxServers,
        maxIps,
        note,
        connections,
        subUsers,
        servers,
        ips,
        additionalEntitlements,
        entitlements,
        blacklist);
  }

  private static @Nullable String getStringOrNull(JsonObject obj, String key) {
    JsonElement el = obj.get(key);
    if (el == null || el.isJsonNull()) return null;
    return el.getAsString();
  }

  private static @Nullable Instant parseInstantOrNull(JsonObject obj, String key) {
    String value = getStringOrNull(obj, key);
    return value != null ? Instant.parse(value) : null;
  }

  private static Map<String, Instant> parseInstantMap(JsonObject obj, String key) {
    Map<String, Instant> map = new LinkedHashMap<>();
    JsonElement el = obj.get(key);
    if (el != null && !el.isJsonNull()) {
      for (Map.Entry<String, JsonElement> entry : el.getAsJsonObject().entrySet()) {
        map.put(entry.getKey(), Instant.parse(entry.getValue().getAsString()));
      }
    }
    return map;
  }

  private static Set<String> parseStringSet(JsonObject obj, String key) {
    Set<String> set = new LinkedHashSet<>();
    JsonElement el = obj.get(key);
    if (el != null && !el.isJsonNull()) {
      for (JsonElement e : el.getAsJsonArray()) {
        set.add(e.getAsString());
      }
    }
    return set;
  }
}
