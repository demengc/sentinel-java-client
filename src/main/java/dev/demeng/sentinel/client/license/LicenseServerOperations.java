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
 * Operations for managing license server identifiers.
 *
 * <p>Obtain an instance via {@link LicenseService#servers()}.
 */
public final class LicenseServerOperations {

  private static final String BASE_PATH = "/api/v2/licenses";

  private final SentinelHttpClient httpClient;
  private final ApiResponseParser apiResponseParser;
  private final LicenseResponseParser licenseResponseParser;
  private final Gson gson;

  LicenseServerOperations(
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
   * Adds server identifiers to a license.
   *
   * @param key the license key
   * @param identifiers the set of server identifiers to add
   * @return the updated license
   * @throws SentinelException if a network or API error occurs
   */
  public License add(String key, Set<String> identifiers) throws SentinelException {
    String json = gson.toJson(identifiers);
    SentinelHttpResponse httpResponse =
        httpClient.request("POST", BASE_PATH + "/" + key + "/servers", json);
    ApiResponseParser.ApiResponse apiResponse = apiResponseParser.parse(httpResponse);
    return licenseResponseParser.parse(apiResponse.result());
  }

  /**
   * Removes server identifiers from a license.
   *
   * @param key the license key
   * @param identifiers the set of server identifiers to remove
   * @return the updated license
   * @throws SentinelException if a network or API error occurs
   */
  public License remove(String key, Set<String> identifiers) throws SentinelException {
    Map<String, List<String>> queryParams = Map.of("servers", List.copyOf(identifiers));
    SentinelHttpResponse httpResponse =
        httpClient.requestWithMultiValuedParams(
            "DELETE", BASE_PATH + "/" + key + "/servers", queryParams);
    ApiResponseParser.ApiResponse apiResponse = apiResponseParser.parse(httpResponse);
    return licenseResponseParser.parse(apiResponse.result());
  }
}
