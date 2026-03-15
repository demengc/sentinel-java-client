package dev.demeng.sentinel.client.internal;

import dev.demeng.sentinel.client.exception.SentinelConnectionException;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.stream.Collectors;

public final class SentinelHttpClient {

  private final HttpClient httpClient;
  private final String baseUrl;
  private final String apiKey;
  private final Duration readTimeout;

  public SentinelHttpClient(
      String baseUrl, String apiKey, Duration connectTimeout, Duration readTimeout) {
    this.baseUrl = stripTrailingSlash(baseUrl);
    this.apiKey = apiKey;
    this.readTimeout = readTimeout;
    this.httpClient = HttpClient.newBuilder().connectTimeout(connectTimeout).build();
  }

  public SentinelHttpClient(
      HttpClient httpClient, String baseUrl, String apiKey, Duration readTimeout) {
    this.httpClient = httpClient;
    this.baseUrl = stripTrailingSlash(baseUrl);
    this.apiKey = apiKey;
    this.readTimeout = readTimeout;
  }

  public SentinelHttpResponse request(String method, String path)
      throws SentinelConnectionException {
    return request(method, path, null, null);
  }

  public SentinelHttpResponse request(String method, String path, String jsonBody)
      throws SentinelConnectionException {
    return request(method, path, jsonBody, null);
  }

  public SentinelHttpResponse request(
      String method, String path, String jsonBody, Map<String, String> queryParams)
      throws SentinelConnectionException {
    String url = baseUrl + path;
    if (queryParams != null && !queryParams.isEmpty()) {
      url +=
          "?"
              + queryParams.entrySet().stream()
                  .sorted(Map.Entry.comparingByKey())
                  .map(e -> encode(e.getKey()) + "=" + encode(e.getValue()))
                  .collect(Collectors.joining("&"));
    }

    HttpRequest.Builder builder =
        HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Authorization", "Bearer " + apiKey)
            .timeout(readTimeout);

    if (jsonBody != null) {
      builder
          .header("Content-Type", "application/json")
          .method(method, HttpRequest.BodyPublishers.ofString(jsonBody));
    } else {
      builder.method(method, HttpRequest.BodyPublishers.noBody());
    }

    try {
      HttpResponse<String> response =
          httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
      return new SentinelHttpResponse(
          response.statusCode(), response.body(), response.headers().map());
    } catch (IOException e) {
      throw new SentinelConnectionException("Failed to connect to Sentinel API", e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new SentinelConnectionException(
          "Request interrupted", new IOException("Interrupted", e));
    }
  }

  private static String stripTrailingSlash(String url) {
    return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
  }

  private static String encode(String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8);
  }
}
