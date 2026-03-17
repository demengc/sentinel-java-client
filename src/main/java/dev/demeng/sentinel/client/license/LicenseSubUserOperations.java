package dev.demeng.sentinel.client.license;

import com.google.gson.Gson;
import dev.demeng.sentinel.client.exception.SentinelException;
import dev.demeng.sentinel.client.internal.ApiResponseParser;
import dev.demeng.sentinel.client.internal.LicenseResponseParser;
import dev.demeng.sentinel.client.internal.SentinelHttpClient;
import dev.demeng.sentinel.client.internal.SentinelHttpResponse;
import java.util.List;

/**
 * Operations for managing license sub-users.
 *
 * <p>Obtain an instance via {@link LicenseService#subUsers()}.
 */
public final class LicenseSubUserOperations {

  private static final String BASE_PATH = "/api/v2/licenses";

  private final SentinelHttpClient httpClient;
  private final ApiResponseParser apiResponseParser;
  private final LicenseResponseParser licenseResponseParser;
  private final Gson gson;

  LicenseSubUserOperations(
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
   * Adds sub-users to a license.
   *
   * @param key the license key
   * @param subUsers the sub-users to add
   * @return the updated license
   * @throws SentinelException if a network or API error occurs
   */
  public License add(String key, List<SubUser> subUsers) throws SentinelException {
    String json = gson.toJson(subUsers);
    SentinelHttpResponse httpResponse =
        httpClient.request("POST", BASE_PATH + "/" + key + "/sub-users", json);
    ApiResponseParser.ApiResponse apiResponse = apiResponseParser.parse(httpResponse);
    return licenseResponseParser.parse(apiResponse.result());
  }

  /**
   * Removes sub-users from a license.
   *
   * <p>Uses POST instead of DELETE because the request body is structured.
   *
   * @param key the license key
   * @param subUsers the sub-users to remove
   * @return the updated license
   * @throws SentinelException if a network or API error occurs
   */
  public License remove(String key, List<SubUser> subUsers) throws SentinelException {
    String json = gson.toJson(subUsers);
    SentinelHttpResponse httpResponse =
        httpClient.request("POST", BASE_PATH + "/" + key + "/sub-users/remove", json);
    ApiResponseParser.ApiResponse apiResponse = apiResponseParser.parse(httpResponse);
    return licenseResponseParser.parse(apiResponse.result());
  }
}
