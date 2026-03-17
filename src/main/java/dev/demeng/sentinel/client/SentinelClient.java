package dev.demeng.sentinel.client;

import com.google.gson.Gson;
import dev.demeng.sentinel.client.internal.ApiResponseParser;
import dev.demeng.sentinel.client.internal.LicenseResponseParser;
import dev.demeng.sentinel.client.internal.PageResponseParser;
import dev.demeng.sentinel.client.internal.ReplayProtector;
import dev.demeng.sentinel.client.internal.SentinelHttpClient;
import dev.demeng.sentinel.client.internal.SignatureVerifier;
import dev.demeng.sentinel.client.license.LicenseService;
import java.time.Duration;
import org.jspecify.annotations.Nullable;

/**
 * Client for the Sentinel API.
 *
 * <p>Use {@link #builder()} to create an instance. The client is thread-safe and should be reused
 * across requests. All license operations are accessed through {@link #licenses()}.
 *
 * <pre>{@code
 * SentinelClient client = SentinelClient.builder()
 *     .baseUrl("https://your-sentinel-instance.com")
 *     .apiKey("your-api-key")
 *     .build();
 *
 * // Validation
 * ValidationResult result = client.licenses().validate(
 *     ValidationRequest.builder()
 *         .product("my-product")
 *         .server("server-1")
 *         .build());
 *
 * // CRUD
 * License license = client.licenses().get("KEY-123");
 * }</pre>
 */
public final class SentinelClient {

  private final SentinelHttpClient httpClient;
  private final ApiResponseParser apiResponseParser;
  private final @Nullable SignatureVerifier signatureVerifier;
  private final @Nullable ReplayProtector replayProtector;
  private final Gson gson;
  private final LicenseService licenseService;

  private SentinelClient(Builder builder) {
    this.httpClient =
        new SentinelHttpClient(
            builder.baseUrl, builder.apiKey, builder.connectTimeout, builder.readTimeout);
    this.apiResponseParser = new ApiResponseParser();
    this.gson = new Gson();

    SignatureVerifier sv = null;
    ReplayProtector rp = null;
    if (builder.publicKey != null) {
      sv = new SignatureVerifier(builder.publicKey);
      rp = new ReplayProtector(builder.replayProtectionWindow, builder.nonceCacheSize);
    }
    this.signatureVerifier = sv;
    this.replayProtector = rp;

    LicenseResponseParser licenseResponseParser = new LicenseResponseParser();
    PageResponseParser pageResponseParser = new PageResponseParser(licenseResponseParser);
    this.licenseService =
        new LicenseService(
            httpClient, apiResponseParser, licenseResponseParser, pageResponseParser, gson, sv, rp);
  }

  SentinelClient(
      SentinelHttpClient httpClient,
      @Nullable SignatureVerifier signatureVerifier,
      @Nullable ReplayProtector replayProtector) {
    this.httpClient = httpClient;
    this.apiResponseParser = new ApiResponseParser();
    this.gson = new Gson();
    this.signatureVerifier = signatureVerifier;
    this.replayProtector = replayProtector;

    LicenseResponseParser licenseResponseParser = new LicenseResponseParser();
    PageResponseParser pageResponseParser = new PageResponseParser(licenseResponseParser);
    this.licenseService =
        new LicenseService(
            httpClient,
            apiResponseParser,
            licenseResponseParser,
            pageResponseParser,
            gson,
            signatureVerifier,
            replayProtector);
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
   * Returns the license operations facade.
   *
   * @return the license service
   */
  public LicenseService licenses() {
    return licenseService;
  }

  /**
   * Builder for constructing {@link SentinelClient} instances.
   *
   * <p>Required fields: {@link #baseUrl(String)} and {@link #apiKey(String)}.
   */
  public static final class Builder {
    private String baseUrl;
    private String apiKey;
    private @Nullable String publicKey;
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
