package dev.demeng.sentinel.client.license;

import com.google.gson.Gson;
import dev.demeng.sentinel.client.exception.SentinelException;
import dev.demeng.sentinel.client.internal.ApiResponseParser;
import dev.demeng.sentinel.client.internal.LicenseResponseParser;
import dev.demeng.sentinel.client.internal.SentinelHttpClient;
import dev.demeng.sentinel.client.internal.SentinelHttpResponse;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Operations for managing license connections (platform-to-user mappings).
 *
 * <p>Obtain an instance via {@link LicenseService#connections()}.
 */
public final class LicenseConnectionOperations {

  private static final String BASE_PATH = "/api/v2/licenses";

  private final SentinelHttpClient httpClient;
  private final ApiResponseParser apiResponseParser;
  private final LicenseResponseParser licenseResponseParser;
  private final Gson gson;

  LicenseConnectionOperations(
      SentinelHttpClient httpClient,
      ApiResponseParser apiResponseParser,
      LicenseResponseParser licenseResponseParser,
      Gson gson) {
    this.httpClient = httpClient;
    this.apiResponseParser = apiResponseParser;
    this.licenseResponseParser = licenseResponseParser;
    this.gson = gson;
  }

  /**
   * Adds connections to a license.
   *
   * @param key the license key
   * @param connections a map of platform names to user identifiers
   * @return the updated license
   * @throws SentinelException if a network or API error occurs
   */
  public License add(String key, Map<String, String> connections) throws SentinelException {
    String json = gson.toJson(connections);
    SentinelHttpResponse httpResponse =
        httpClient.request("POST", BASE_PATH + "/" + key + "/connections", json);
    ApiResponseParser.ApiResponse apiResponse = apiResponseParser.parse(httpResponse);
    return licenseResponseParser.parse(apiResponse.result());
  }

  /**
   * Removes connections from a license by platform.
   *
   * @param key the license key
   * @param platforms the set of platform names to disconnect
   * @return the updated license
   * @throws SentinelException if a network or API error occurs
   */
  public License remove(String key, Set<String> platforms) throws SentinelException {
    Map<String, List<String>> queryParams = Map.of("platforms", List.copyOf(platforms));
    SentinelHttpResponse httpResponse =
        httpClient.requestWithMultiValuedParams(
            "DELETE", BASE_PATH + "/" + key + "/connections", queryParams);
    ApiResponseParser.ApiResponse apiResponse = apiResponseParser.parse(httpResponse);
    return licenseResponseParser.parse(apiResponse.result());
  }
}
