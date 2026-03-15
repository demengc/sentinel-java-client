package dev.demeng.sentinel.client;

import com.google.gson.Gson;
import dev.demeng.sentinel.client.exception.SentinelException;
import dev.demeng.sentinel.client.internal.ApiResponseParser;
import dev.demeng.sentinel.client.internal.ReplayProtector;
import dev.demeng.sentinel.client.internal.SentinelHttpClient;
import dev.demeng.sentinel.client.internal.SentinelHttpResponse;
import dev.demeng.sentinel.client.internal.SignatureVerifier;
import dev.demeng.sentinel.client.internal.ValidationResponseParser;
import dev.demeng.sentinel.client.validation.ValidationRequest;
import dev.demeng.sentinel.client.validation.ValidationResult;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Client for the Sentinel API.
 *
 * <p>Use {@link #builder()} to create an instance. The client is thread-safe and should be reused
 * across requests.
 *
 * <pre>{@code
 * SentinelClient client = SentinelClient.builder()
 *     .baseUrl("https://your-sentinel-instance.com")
 *     .apiKey("your-api-key")
 *     .build();
 *
 * ValidationResult result = client.validate(
 *     ValidationRequest.builder()
 *         .product("my-product")
 *         .server("server-1")
 *         .build());
 * }</pre>
 */
public final class SentinelClient {

  private final SentinelHttpClient httpClient;
  private final ApiResponseParser apiResponseParser;
  private final ValidationResponseParser validationResponseParser;
  private final SignatureVerifier signatureVerifier;
  private final ReplayProtector replayProtector;
  private final Gson gson;

  private SentinelClient(Builder builder) {
    this.httpClient =
        new SentinelHttpClient(
            builder.baseUrl, builder.apiKey, builder.connectTimeout, builder.readTimeout);
    this.apiResponseParser = new ApiResponseParser();
    this.validationResponseParser = new ValidationResponseParser();
    this.gson = new Gson();

    if (builder.publicKey != null) {
      this.signatureVerifier = new SignatureVerifier(builder.publicKey);
      this.replayProtector =
          new ReplayProtector(builder.replayProtectionWindow, builder.nonceCacheSize);
    } else {
      this.signatureVerifier = null;
      this.replayProtector = null;
    }
  }

  SentinelClient(
      SentinelHttpClient httpClient,
      SignatureVerifier signatureVerifier,
      ReplayProtector replayProtector) {
    this.httpClient = httpClient;
    this.apiResponseParser = new ApiResponseParser();
    this.validationResponseParser = new ValidationResponseParser();
    this.gson = new Gson();
    this.signatureVerifier = signatureVerifier;
    this.replayProtector = replayProtector;
  }

  /**
   * Creates a new {@link Builder} for constructing a {@code SentinelClient}.
   *
   * @return a new builder instance
   */
  public static Builder builder() {
    return new Builder();
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
    SentinelHttpResponse httpResponse =
        httpClient.request("POST", "/api/v2/licenses/validate", json);
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

  /**
   * Builder for constructing {@link SentinelClient} instances.
   *
   * <p>Required fields: {@link #baseUrl(String)} and {@link #apiKey(String)}.
   */
  public static final class Builder {
    private String baseUrl;
    private String apiKey;
    private String publicKey;
    private Duration connectTimeout = Duration.ofSeconds(5);
    private Duration readTimeout = Duration.ofSeconds(10);
    private Duration replayProtectionWindow = Duration.ofSeconds(30);
    private int nonceCacheSize = 1000;

    private Builder() {}

    /**
     * Sets the base URL of the Sentinel API (e.g. {@code "https://your-sentinel-instance.com"}).
     *
     * @param baseUrl the base URL (required)
     * @return this builder
     */
    public Builder baseUrl(String baseUrl) {
      this.baseUrl = baseUrl;
      return this;
    }

    /**
     * Sets the API key used to authenticate requests.
     *
     * @param apiKey the API key (required)
     * @return this builder
     */
    public Builder apiKey(String apiKey) {
      this.apiKey = apiKey;
      return this;
    }

    /**
     * Sets the Base64-encoded Ed25519 public key for response signature verification. When set, all
     * successful validation responses will be cryptographically verified and checked for replay
     * attacks.
     *
     * @param publicKey the Base64-encoded Ed25519 public key, or {@code null} to disable
     * @return this builder
     */
    public Builder publicKey(String publicKey) {
      this.publicKey = publicKey;
      return this;
    }

    /**
     * Sets the connection timeout. Defaults to 5 seconds.
     *
     * @param connectTimeout the connection timeout
     * @return this builder
     */
    public Builder connectTimeout(Duration connectTimeout) {
      this.connectTimeout = connectTimeout;
      return this;
    }

    /**
     * Sets the read timeout. Defaults to 10 seconds.
     *
     * @param readTimeout the read timeout
     * @return this builder
     */
    public Builder readTimeout(Duration readTimeout) {
      this.readTimeout = readTimeout;
      return this;
    }

    /**
     * Sets the maximum age of a response timestamp for replay protection. Responses with timestamps
     * outside this window are rejected. Defaults to 30 seconds. Only applies when signature
     * verification is enabled.
     *
     * @param replayProtectionWindow the replay protection time window
     * @return this builder
     */
    public Builder replayProtectionWindow(Duration replayProtectionWindow) {
      this.replayProtectionWindow = replayProtectionWindow;
      return this;
    }

    /**
     * Sets the maximum number of nonces to cache for replay detection. Defaults to 1000. Only
     * applies when signature verification is enabled.
     *
     * @param nonceCacheSize the maximum nonce cache size
     * @return this builder
     */
    public Builder nonceCacheSize(int nonceCacheSize) {
      this.nonceCacheSize = nonceCacheSize;
      return this;
    }

    /**
     * Builds and returns a new {@link SentinelClient}.
     *
     * @return a configured client instance
     * @throws IllegalStateException if required fields are missing or the public key is invalid
     */
    public SentinelClient build() {
      if (baseUrl == null || baseUrl.isEmpty()) {
        throw new IllegalStateException("baseUrl is required");
      }
      if (apiKey == null || apiKey.isEmpty()) {
        throw new IllegalStateException("apiKey is required");
      }
      if (publicKey != null) {
        try {
          SignatureVerifier.parsePublicKey(publicKey);
        } catch (Exception e) {
          throw new IllegalStateException(
              "publicKey is not a valid Base64-encoded Ed25519 public key", e);
        }
      }
      return new SentinelClient(this);
    }
  }
}
