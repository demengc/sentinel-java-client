package dev.demeng.sentinel.client.internal;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public record SentinelHttpResponse(int statusCode, String body, Map<String, List<String>> headers) {

  public String firstHeader(String name) {
    String lowerName = name.toLowerCase(Locale.ROOT);
    for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
      if (entry.getKey().toLowerCase(Locale.ROOT).equals(lowerName)) {
        List<String> values = entry.getValue();
        return (values != null && !values.isEmpty()) ? values.get(0) : null;
      }
    }
    return null;
  }
}
