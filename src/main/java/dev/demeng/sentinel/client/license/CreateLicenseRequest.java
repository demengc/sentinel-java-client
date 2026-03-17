package dev.demeng.sentinel.client.license;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import org.jspecify.annotations.Nullable;

public final class CreateLicenseRequest {

  private final String product;
  private final @Nullable String key;
  private final @Nullable String tier;
  private final @Nullable Instant expiration;
  private final @Nullable Integer maxServers;
  private final @Nullable Integer maxIps;
  private final @Nullable String note;
  private final @Nullable Map<String, String> connections;
  private final @Nullable Set<String> additionalEntitlements;

  private CreateLicenseRequest(Builder builder) {
    this.product = builder.product;
    this.key = builder.key;
    this.tier = builder.tier;
    this.expiration = builder.expiration;
    this.maxServers = builder.maxServers;
    this.maxIps = builder.maxIps;
    this.note = builder.note;
    this.connections = builder.connections;
    this.additionalEntitlements = builder.additionalEntitlements;
  }

  public static Builder builder() {
    return new Builder();
  }

  public String getProduct() {
    return product;
  }

  public @Nullable String getKey() {
    return key;
  }

  public @Nullable String getTier() {
    return tier;
  }

  public @Nullable Instant getExpiration() {
    return expiration;
  }

  public @Nullable Integer getMaxServers() {
    return maxServers;
  }

  public @Nullable Integer getMaxIps() {
    return maxIps;
  }

  public @Nullable String getNote() {
    return note;
  }

  public @Nullable Map<String, String> getConnections() {
    return connections;
  }

  public @Nullable Set<String> getAdditionalEntitlements() {
    return additionalEntitlements;
  }

  public static final class Builder {

    private @Nullable String product;
    private @Nullable String key;
    private @Nullable String tier;
    private @Nullable Instant expiration;
    private @Nullable Integer maxServers;
    private @Nullable Integer maxIps;
    private @Nullable String note;
    private @Nullable Map<String, String> connections;
    private @Nullable Set<String> additionalEntitlements;

    private Builder() {}

    public Builder product(String product) {
      this.product = product;
      return this;
    }

    public Builder key(String key) {
      this.key = key;
      return this;
    }

    public Builder tier(String tier) {
      this.tier = tier;
      return this;
    }

    public Builder expiration(Instant expiration) {
      this.expiration = expiration;
      return this;
    }

    public Builder maxServers(int maxServers) {
      this.maxServers = maxServers;
      return this;
    }

    public Builder maxIps(int maxIps) {
      this.maxIps = maxIps;
      return this;
    }

    public Builder note(String note) {
      this.note = note;
      return this;
    }

    public Builder connections(Map<String, String> connections) {
      this.connections = connections;
      return this;
    }

    public Builder additionalEntitlements(Set<String> additionalEntitlements) {
      this.additionalEntitlements = additionalEntitlements;
      return this;
    }

    public CreateLicenseRequest build() {
      if (product == null || product.isEmpty()) {
        throw new IllegalStateException("product is required");
      }
      return new CreateLicenseRequest(this);
    }
  }
}
