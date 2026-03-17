package dev.demeng.sentinel.client.internal;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import dev.demeng.sentinel.client.exception.SentinelApiException;
import org.jspecify.annotations.Nullable;

public final class ApiResponseParser {

  public record ApiResponse(
      int httpStatus,
      @Nullable String type,
      @Nullable String message,
      @Nullable JsonObject result) {}

  public ApiResponse parse(SentinelHttpResponse response, int... allowedStatuses)
      throws SentinelApiException {
    if (response.body() == null) {
      throw new SentinelApiException(response.statusCode(), null, "Empty response body");
    }

    JsonObject root;
    try {
      root = JsonParser.parseString(response.body()).getAsJsonObject();
    } catch (JsonParseException | IllegalStateException e) {
      throw new SentinelApiException(
          response.statusCode(), null, "Failed to parse response body", e);
    }

    String type = getStringOrNull(root, "type");
    String message = getStringOrNull(root, "message");
    JsonElement resultEl = root.get("result");
    JsonObject result =
        (resultEl != null && resultEl.isJsonObject()) ? resultEl.getAsJsonObject() : null;

    int status = response.statusCode();
    if (status >= 200 && status < 300) {
      return new ApiResponse(status, type, message, result);
    }

    for (int allowed : allowedStatuses) {
      if (status == allowed) {
        return new ApiResponse(status, type, message, result);
      }
    }

    int retryAfter = -1;
    if (status == 429) {
      String retryHeader = response.firstHeader("X-Rate-Limit-Retry-After-Seconds");
      if (retryHeader != null) {
        try {
          retryAfter = Integer.parseInt(retryHeader);
        } catch (NumberFormatException ignored) {
        }
      }
    }

    throw new SentinelApiException(status, type, message, retryAfter);
  }

  private static @Nullable String getStringOrNull(JsonObject obj, String key) {
    JsonElement el = obj.get(key);
    if (el == null || el.isJsonNull()) return null;
    return el.getAsString();
  }
}
