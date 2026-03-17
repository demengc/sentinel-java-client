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
 * Operations for managing license IP addresses.
 *
 * <p>Obtain an instance via {@link LicenseService#ips()}.
 */
public final class LicenseIpOperations {

  private static final String BASE_PATH = "/api/v2/licenses";

  private final SentinelHttpClient httpClient;
  private final ApiResponseParser apiResponseParser;
  private final LicenseResponseParser licenseResponseParser;
  private final Gson gson;

  LicenseIpOperations(
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
   * Adds IP addresses to a license.
   *
   * @param key the license key
   * @param addresses the set of IP addresses to add
   * @return the updated license
   * @throws SentinelException if a network or API error occurs
   */
  public License add(String key, Set<String> addresses) throws SentinelException {
    String json = gson.toJson(addresses);
    SentinelHttpResponse httpResponse =
        httpClient.request("POST", BASE_PATH + "/" + key + "/ips", json);
    ApiResponseParser.ApiResponse apiResponse = apiResponseParser.parse(httpResponse);
    return licenseResponseParser.parse(apiResponse.result());
  }

  /**
   * Removes IP addresses from a license.
   *
   * @param key the license key
   * @param addresses the set of IP addresses to remove
   * @return the updated license
   * @throws SentinelException if a network or API error occurs
   */
  public License remove(String key, Set<String> addresses) throws SentinelException {
    Map<String, List<String>> queryParams = Map.of("ips", List.copyOf(addresses));
    SentinelHttpResponse httpResponse =
        httpClient.requestWithMultiValuedParams(
            "DELETE", BASE_PATH + "/" + key + "/ips", queryParams);
    ApiResponseParser.ApiResponse apiResponse = apiResponseParser.parse(httpResponse);
    return licenseResponseParser.parse(apiResponse.result());
  }
}
