package dev.demeng.sentinel.client.internal;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.demeng.sentinel.client.exception.SentinelApiException;
import dev.demeng.sentinel.client.license.validation.FailureDetails;
import dev.demeng.sentinel.client.license.validation.ValidationDetails;
import dev.demeng.sentinel.client.license.validation.ValidationResult;
import dev.demeng.sentinel.client.license.validation.ValidationResultType;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.jspecify.annotations.Nullable;

public final class ValidationResponseParser {

  public record ParsedValidationResponse(
      ValidationResult result,
      @Nullable String nonce,
      long timestamp,
      @Nullable String signature,
      @Nullable String expiration,
      int serverCount,
      int maxServers,
      int ipCount,
      int maxIps,
      @Nullable String tier,
      @Nullable List<String> entitlements) {}

  public ParsedValidationResponse parse(ApiResponseParser.ApiResponse response)
      throws SentinelApiException {
    if (response.httpStatus() == 200) {
      return parseSuccess(response);
    }

    if (response.httpStatus() == 403) {
      ValidationResultType resultType = ValidationResultType.fromString(response.type());
      if (resultType == ValidationResultType.UNKNOWN) {
        throw new SentinelApiException(response.httpStatus(), response.type(), response.message());
      }
      FailureDetails failureDetails = parseFailureDetails(resultType, response.result());
      return new ParsedValidationResponse(
          ValidationResult.failure(resultType, response.message(), failureDetails),
          null,
          0,
          null,
          null,
          0,
          0,
          0,
          0,
          null,
          null);
    }

    throw new SentinelApiException(response.httpStatus(), response.type(), response.message());
  }

  private ParsedValidationResponse parseSuccess(ApiResponseParser.ApiResponse response)
      throws SentinelApiException {
    try {
      JsonObject result = response.result();
      JsonObject validation = result.getAsJsonObject("validation");
      String nonce = validation.get("nonce").getAsString();
      long timestamp = validation.get("timestamp").getAsLong();
      String signature = getStringOrNull(validation, "signature");

      JsonObject detailsObj = validation.getAsJsonObject("details");
      String expirationStr = getStringOrNull(detailsObj, "expiration");
      Instant expiration = expirationStr != null ? Instant.parse(expirationStr) : null;
      int serverCount = detailsObj.get("serverCount").getAsInt();
      int maxServers = detailsObj.get("maxServers").getAsInt();
      int ipCount = detailsObj.get("ipCount").getAsInt();
      int maxIps = detailsObj.get("maxIps").getAsInt();
      String tier = detailsObj.get("tier").getAsString();

      List<String> entitlementsList = null;
      Set<String> entitlementsSet;
      JsonElement entitlementsEl = detailsObj.get("entitlements");
      if (entitlementsEl != null && !entitlementsEl.isJsonNull()) {
        entitlementsList = new ArrayList<>();
        for (JsonElement el : entitlementsEl.getAsJsonArray()) {
          entitlementsList.add(el.getAsString());
        }
        entitlementsSet = new LinkedHashSet<>(entitlementsList);
      } else {
        entitlementsSet = Set.of();
      }

      ValidationDetails details =
          new ValidationDetails(
              expiration, serverCount, maxServers, ipCount, maxIps, tier, entitlementsSet);

      return new ParsedValidationResponse(
          ValidationResult.success(details, response.message()),
          nonce,
          timestamp,
          signature,
          expirationStr,
          serverCount,
          maxServers,
          ipCount,
          maxIps,
          tier,
          entitlementsList);
    } catch (Exception e) {
      throw new SentinelApiException(200, null, "Failed to parse validation response", e);
    }
  }

  private static @Nullable FailureDetails parseFailureDetails(
      ValidationResultType type, @Nullable JsonObject result) {
    if (result == null) return null;
    try {
      return switch (type) {
        case BLACKLISTED_LICENSE -> {
          JsonObject blacklist = result.getAsJsonObject("blacklist");
          Instant timestamp = Instant.parse(blacklist.get("timestamp").getAsString());
          String reason = getStringOrNull(blacklist, "reason");
          yield new FailureDetails.BlacklistDetails(timestamp, reason);
        }
        case EXCESSIVE_SERVERS ->
            new FailureDetails.ExcessiveServersDetails(result.get("maxServers").getAsInt());
        case EXCESSIVE_IPS ->
            new FailureDetails.ExcessiveIpsDetails(result.get("maxIps").getAsInt());
        default -> null;
      };
    } catch (Exception e) {
      return null;
    }
  }

  private static @Nullable String getStringOrNull(JsonObject obj, String key) {
    JsonElement el = obj.get(key);
    if (el == null || el.isJsonNull()) return null;
    return el.getAsString();
  }
}
