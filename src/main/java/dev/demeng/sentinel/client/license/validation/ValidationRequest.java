package dev.demeng.sentinel.client.license.validation;

import org.jspecify.annotations.Nullable;

/**
 * Request parameters for a license validation call.
 *
 * <p>Use {@link #builder()} to construct an instance. The {@code product} and {@code server} fields
 * are required; all others are optional.
 */
public final class ValidationRequest {

  private final @Nullable String key;
  private final String product;
  private final String server;
  private final @Nullable String ip;
  private final @Nullable String connectionPlatform;
  private final @Nullable String connectionValue;

  private ValidationRequest(Builder builder) {
    this.key = builder.key;
    this.product = builder.product;
    this.server = builder.server;
    this.ip = builder.ip;
    this.connectionPlatform = builder.connectionPlatform;
    this.connectionValue = builder.connectionValue;
  }

  /**
   * Creates a new {@link Builder} for constructing a {@code ValidationRequest}.
   *
   * @return a new builder instance
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * @return the license key, or {@code null} if not set
   */
  public @Nullable String getKey() {
    return key;
  }

  /**
   * @return the product identifier
   */
  public String getProduct() {
    return product;
  }

  /**
   * @return the server identifier
   */
  public String getServer() {
    return server;
  }

  /**
   * @return the client IP address, or {@code null} if not set
   */
  public @Nullable String getIp() {
    return ip;
  }

  /**
   * @return the connection platform (e.g. {@code "discord"}), or {@code null} if not set
   */
  public @Nullable String getConnectionPlatform() {
    return connectionPlatform;
  }

  /**
   * @return the connection value (e.g. a user ID), or {@code null} if not set
   */
  public @Nullable String getConnectionValue() {
    return connectionValue;
  }

  /**
   * Builder for constructing {@link ValidationRequest} instances.
   *
   * <p>Required fields: {@link #product(String)} and {@link #server(String)}.
   */
  public static final class Builder {
    private @Nullable String key;
    private String product;
    private String server;
    private @Nullable String ip;
    private @Nullable String connectionPlatform;
    private @Nullable String connectionValue;

    private Builder() {}

    /**
     * Sets the license key to validate.
     *
     * @param key the license key
     * @return this builder
     */
    public Builder key(String key) {
      this.key = key;
      return this;
    }

    /**
     * Sets the product identifier. Required.
     *
     * @param product the product identifier
     * @return this builder
     */
    public Builder product(String product) {
      this.product = product;
      return this;
    }

    /**
     * Sets the server identifier. Required.
     *
     * @param server the server identifier
     * @return this builder
     */
    public Builder server(String server) {
      this.server = server;
      return this;
    }

    /**
     * Sets the client IP address.
     *
     * @param ip the client IP address
     * @return this builder
     */
    public Builder ip(String ip) {
      this.ip = ip;
      return this;
    }

    /**
     * Sets the connection platform (e.g. {@code "discord"}).
     *
     * @param connectionPlatform the connection platform
     * @return this builder
     */
    public Builder connectionPlatform(String connectionPlatform) {
      this.connectionPlatform = connectionPlatform;
      return this;
    }

    /**
     * Sets the connection value (e.g. a Discord user ID).
     *
     * @param connectionValue the connection value
     * @return this builder
     */
    public Builder connectionValue(String connectionValue) {
      this.connectionValue = connectionValue;
      return this;
    }

    /**
     * Builds and returns a new {@link ValidationRequest}.
     *
     * @return a configured request instance
     * @throws IllegalStateException if required fields are missing
     */
    public ValidationRequest build() {
      if (product == null || product.isEmpty()) {
        throw new IllegalStateException("product is required");
      }
      if (server == null || server.isEmpty()) {
        throw new IllegalStateException("server is required");
      }
      return new ValidationRequest(this);
    }
  }
}
