package dev.demeng.sentinel.client.license;

import com.google.gson.Gson;
import dev.demeng.sentinel.client.exception.SentinelException;
import dev.demeng.sentinel.client.internal.ApiResponseParser;
import dev.demeng.sentinel.client.internal.LicenseResponseParser;
import dev.demeng.sentinel.client.internal.PageResponseParser;
import dev.demeng.sentinel.client.internal.ReplayProtector;
import dev.demeng.sentinel.client.internal.SentinelHttpClient;
import dev.demeng.sentinel.client.internal.SentinelHttpResponse;
import dev.demeng.sentinel.client.internal.SignatureVerifier;
import dev.demeng.sentinel.client.internal.ValidationResponseParser;
import dev.demeng.sentinel.client.license.validation.ValidationRequest;
import dev.demeng.sentinel.client.license.validation.ValidationResult;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * Facade for all license operations against the Sentinel API.
 *
 * <p>Obtain an instance via {@link dev.demeng.sentinel.client.SentinelClient#licenses()}.
 */
public final class LicenseService {

  private static final String BASE_PATH = "/api/v2/licenses";

  private final SentinelHttpClient httpClient;
  private final ApiResponseParser apiResponseParser;
  private final LicenseResponseParser licenseResponseParser;
  private final PageResponseParser pageResponseParser;
  private final ValidationResponseParser validationResponseParser;
  private final Gson gson;
  private final @Nullable SignatureVerifier signatureVerifier;
  private final @Nullable ReplayProtector replayProtector;
  private final LicenseConnectionOperations connectionOperations;
  private final LicenseSubUserOperations subUserOperations;
  private final LicenseServerOperations serverOperations;
  private final LicenseIpOperations ipOperations;

  public LicenseService(
      SentinelHttpClient httpClient,
      ApiResponseParser apiResponseParser,
      LicenseResponseParser licenseResponseParser,
      PageResponseParser pageResponseParser,
      Gson gson,
      @Nullable SignatureVerifier signatureVerifier,
      @Nullable ReplayProtector replayProtector) {
    this.httpClient = httpClient;
    this.apiResponseParser = apiResponseParser;
    this.licenseResponseParser = licenseResponseParser;
    this.pageResponseParser = pageResponseParser;
    this.validationResponseParser = new ValidationResponseParser();
    this.gson = gson;
    this.signatureVerifier = signatureVerifier;
    this.replayProtector = replayProtector;
    this.connectionOperations =
        new LicenseConnectionOperations(httpClient, apiResponseParser, licenseResponseParser, gson);
    this.subUserOperations =
        new LicenseSubUserOperations(httpClient, apiResponseParser, licenseResponseParser, gson);
    this.serverOperations =
        new LicenseServerOperations(httpClient, apiResponseParser, licenseResponseParser, gson);
    this.ipOperations =
        new LicenseIpOperations(httpClient, apiResponseParser, licenseResponseParser, gson);
  }

  /**
   * Returns the operations handle for managing license connections.
   *
   * @return the connection operations
   */
  public LicenseConnectionOperations connections() {
    return connectionOperations;
  }

  /**
   * Returns the operations handle for managing license sub-users.
   *
   * @return the sub-user operations
   */
  public LicenseSubUserOperations subUsers() {
    return subUserOperations;
  }

  /**
   * Returns the operations handle for managing license server identifiers.
   *
   * @return the server operations
   */
  public LicenseServerOperations servers() {
    return serverOperations;
  }

  /**
   * Returns the operations handle for managing license IP addresses.
   *
   * @return the IP operations
   */
  public LicenseIpOperations ips() {
    return ipOperations;
  }

  /**
   * Validates a license key against the Sentinel API.
   *
   * <p>On validation failure (e.g. expired or blacklisted license), this method returns a {@link
   * ValidationResult} with {@link ValidationResult#isValid()} returning {@code false} rather than
   * throwing an exception. Use {@link ValidationResult#requireValid()} to throw on failure instead.
   *
   * <p>If signature verification is enabled, the response signature and nonce are verified before
   * returning. Tampered or replayed responses will throw the appropriate exception.
   *
   * @param request the validation request containing the license key and context
   * @return the validation result
   * @throws SentinelException if a network, API, signature, or replay error occurs
   */
  public ValidationResult validate(ValidationRequest request) throws SentinelException {
    String json = buildValidationJson(request);
    SentinelHttpResponse httpResponse = httpClient.request("POST", BASE_PATH + "/validate", json);
    ApiResponseParser.ApiResponse apiResponse = apiResponseParser.parse(httpResponse, 403);
    ValidationResponseParser.ParsedValidationResponse parsed =
        validationResponseParser.parse(apiResponse);
    ValidationResult result = parsed.result();

    if (result.isValid() && signatureVerifier != null) {
      signatureVerifier.verify(
          parsed.signature(),
          parsed.nonce(),
          parsed.timestamp(),
          parsed.expiration(),
          parsed.serverCount(),
          parsed.maxServers(),
          parsed.ipCount(),
          parsed.maxIps(),
          parsed.tier(),
          parsed.entitlements());
      replayProtector.check(parsed.nonce(), parsed.timestamp());
    }

    return result;
  }

  /**
   * Creates a new license.
   *
   * @param request the creation parameters
   * @return the created license
   * @throws SentinelException if a network or API error occurs
   */
  public License create(CreateLicenseRequest request) throws SentinelException {
    String json = buildCreateJson(request);
    SentinelHttpResponse httpResponse = httpClient.request("POST", BASE_PATH, json);
    ApiResponseParser.ApiResponse apiResponse = apiResponseParser.parse(httpResponse);
    return licenseResponseParser.parse(apiResponse.result());
  }

  /**
   * Retrieves a license by key.
   *
   * @param key the license key
   * @return the license
   * @throws SentinelException if a network or API error occurs
   */
  public License get(String key) throws SentinelException {
    SentinelHttpResponse httpResponse = httpClient.request("GET", BASE_PATH + "/" + key);
    ApiResponseParser.ApiResponse apiResponse = apiResponseParser.parse(httpResponse);
    return licenseResponseParser.parse(apiResponse.result());
  }

  /**
   * Lists licenses matching the given criteria.
   *
   * @param request the filter and pagination parameters
   * @return a page of licenses
   * @throws SentinelException if a network or API error occurs
   */
  public Page<License> list(ListLicensesRequest request) throws SentinelException {
    SentinelHttpResponse httpResponse =
        httpClient.request("GET", BASE_PATH, null, request.toQueryParams());
    ApiResponseParser.ApiResponse apiResponse = apiResponseParser.parse(httpResponse);
    return pageResponseParser.parse(apiResponse.result());
  }

  /**
   * Partially updates a license.
   *
   * @param key the license key
   * @param request the fields to update
   * @return the updated license
   * @throws SentinelException if a network or API error occurs
   */
  public License update(String key, UpdateLicenseRequest request) throws SentinelException {
    String json = buildUpdateJson(request);
    SentinelHttpResponse httpResponse = httpClient.request("PATCH", BASE_PATH + "/" + key, json);
    ApiResponseParser.ApiResponse apiResponse = apiResponseParser.parse(httpResponse);
    return licenseResponseParser.parse(apiResponse.result());
  }

  /**
   * Deletes a license.
   *
   * @param key the license key
   * @throws SentinelException if a network or API error occurs
   */
  public void delete(String key) throws SentinelException {
    SentinelHttpResponse httpResponse = httpClient.request("DELETE", BASE_PATH + "/" + key);
    if (httpResponse.statusCode() == 204) {
      return;
    }
    apiResponseParser.parse(httpResponse);
  }

  /**
   * Regenerates the key for a license, assigning a random new key.
   *
   * @param key the current license key
   * @return the license with its new key
   * @throws SentinelException if a network or API error occurs
   */
  public License regenerateKey(String key) throws SentinelException {
    return regenerateKey(key, null);
  }

  /**
   * Regenerates the key for a license, optionally specifying the new key.
   *
   * @param key the current license key
   * @param newKey the desired new key, or {@code null} for a random key
   * @return the license with its new key
   * @throws SentinelException if a network or API error occurs
   */
  public License regenerateKey(String key, @Nullable String newKey) throws SentinelException {
    Map<String, String> queryParams = newKey != null ? Map.of("newKey", newKey) : Map.of();
    SentinelHttpResponse httpResponse =
        httpClient.request("POST", BASE_PATH + "/" + key + "/regenerate-key", null, queryParams);
    ApiResponseParser.ApiResponse apiResponse = apiResponseParser.parse(httpResponse);
    return licenseResponseParser.parse(apiResponse.result());
  }

  private String buildValidationJson(ValidationRequest request) {
    Map<String, String> body = new LinkedHashMap<>();
    if (request.getKey() != null) body.put("key", request.getKey());
    body.put("product", request.getProduct());
    body.put("server", request.getServer());
    if (request.getIp() != null) body.put("ip", request.getIp());
    if (request.getConnectionPlatform() != null)
      body.put("connectionPlatform", request.getConnectionPlatform());
    if (request.getConnectionValue() != null)
      body.put("connectionValue", request.getConnectionValue());
    return gson.toJson(body);
  }

  private String buildCreateJson(CreateLicenseRequest request) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("product", request.getProduct());
    if (request.getKey() != null) body.put("key", request.getKey());
    if (request.getTier() != null) body.put("tier", request.getTier());
    if (request.getExpiration() != null) body.put("expiration", request.getExpiration().toString());
    if (request.getMaxServers() != null) body.put("maxServers", request.getMaxServers());
    if (request.getMaxIps() != null) body.put("maxIps", request.getMaxIps());
    if (request.getNote() != null) body.put("note", request.getNote());
    if (request.getConnections() != null) body.put("connections", request.getConnections());
    if (request.getAdditionalEntitlements() != null)
      body.put("additionalEntitlements", request.getAdditionalEntitlements());
    return gson.toJson(body);
  }

  private String buildUpdateJson(UpdateLicenseRequest request) {
    Map<String, Object> body = new LinkedHashMap<>();
    for (String field : request.getSetFields()) {
      switch (field) {
        case "product" -> body.put("product", request.getProduct());
        case "tier" -> body.put("tier", request.getTier());
        case "expiration" -> {
          Instant exp = request.getExpiration();
          body.put("expiration", exp != null ? exp.toString() : null);
        }
        case "maxServers" -> body.put("maxServers", request.getMaxServers());
        case "maxIps" -> body.put("maxIps", request.getMaxIps());
        case "note" -> body.put("note", request.getNote());
        case "blacklistReason" -> body.put("blacklistReason", request.getBlacklistReason());
        case "connections" -> body.put("connections", request.getConnections());
        case "subUsers" -> body.put("subUsers", request.getSubUsers());
        case "servers" -> body.put("servers", request.getServers());
        case "ips" -> body.put("ips", request.getIps());
        case "additionalEntitlements" ->
            body.put("additionalEntitlements", request.getAdditionalEntitlements());
        default -> {}
      }
    }
    return gson.toJson(body);
  }
}
