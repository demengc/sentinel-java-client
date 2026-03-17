package dev.demeng.sentinel.client.license;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jspecify.annotations.Nullable;

public final class UpdateLicenseRequest {

  private final Set<String> setFields;
  private final @Nullable String product;
  private final @Nullable String tier;
  private final @Nullable Instant expiration;
  private final @Nullable Integer maxServers;
  private final @Nullable Integer maxIps;
  private final @Nullable String note;
  private final @Nullable String blacklistReason;
  private final @Nullable Map<String, String> connections;
  private final @Nullable List<SubUser> subUsers;
  private final @Nullable Set<String> servers;
  private final @Nullable Set<String> ips;
  private final @Nullable Set<String> additionalEntitlements;

  private UpdateLicenseRequest(Builder builder) {
    this.setFields = Set.copyOf(builder.setFields);
    this.product = builder.product;
    this.tier = builder.tier;
    this.expiration = builder.expiration;
    this.maxServers = builder.maxServers;
    this.maxIps = builder.maxIps;
    this.note = builder.note;
    this.blacklistReason = builder.blacklistReason;
    this.connections = builder.connections;
    this.subUsers = builder.subUsers;
    this.servers = builder.servers;
    this.ips = builder.ips;
    this.additionalEntitlements = builder.additionalEntitlements;
  }

  public static Builder builder() {
    return new Builder();
  }

  public boolean isSet(String field) {
    return setFields.contains(field);
  }

  public Set<String> getSetFields() {
    return setFields;
  }

  public @Nullable String getProduct() {
    return product;
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

  public @Nullable String getBlacklistReason() {
    return blacklistReason;
  }

  public @Nullable Map<String, String> getConnections() {
    return connections;
  }

  public @Nullable List<SubUser> getSubUsers() {
    return subUsers;
  }

  public @Nullable Set<String> getServers() {
    return servers;
  }

  public @Nullable Set<String> getIps() {
    return ips;
  }

  public @Nullable Set<String> getAdditionalEntitlements() {
    return additionalEntitlements;
  }

  public static final class Builder {

    private final Set<String> setFields = new LinkedHashSet<>();
    private @Nullable String product;
    private @Nullable String tier;
    private @Nullable Instant expiration;
    private @Nullable Integer maxServers;
    private @Nullable Integer maxIps;
    private @Nullable String note;
    private @Nullable String blacklistReason;
    private @Nullable Map<String, String> connections;
    private @Nullable List<SubUser> subUsers;
    private @Nullable Set<String> servers;
    private @Nullable Set<String> ips;
    private @Nullable Set<String> additionalEntitlements;

    private Builder() {}

    public Builder product(String product) {
      this.product = product;
      setFields.add("product");
      return this;
    }

    public Builder tier(String tier) {
      this.tier = tier;
      setFields.add("tier");
      return this;
    }

    public Builder expiration(Instant expiration) {
      this.expiration = expiration;
      setFields.add("expiration");
      return this;
    }

    public Builder clearExpiration() {
      this.expiration = Instant.EPOCH;
      setFields.add("expiration");
      return this;
    }

    public Builder maxServers(int maxServers) {
      this.maxServers = maxServers;
      setFields.add("maxServers");
      return this;
    }

    public Builder maxIps(int maxIps) {
      this.maxIps = maxIps;
      setFields.add("maxIps");
      return this;
    }

    public Builder note(String note) {
      this.note = note;
      setFields.add("note");
      return this;
    }

    public Builder blacklistReason(String blacklistReason) {
      this.blacklistReason = blacklistReason;
      setFields.add("blacklistReason");
      return this;
    }

    public Builder connections(Map<String, String> connections) {
      this.connections = connections;
      setFields.add("connections");
      return this;
    }

    public Builder subUsers(List<SubUser> subUsers) {
      this.subUsers = subUsers;
      setFields.add("subUsers");
      return this;
    }

    public Builder servers(Set<String> servers) {
      this.servers = servers;
      setFields.add("servers");
      return this;
    }

    public Builder ips(Set<String> ips) {
      this.ips = ips;
      setFields.add("ips");
      return this;
    }

    public Builder additionalEntitlements(Set<String> additionalEntitlements) {
      this.additionalEntitlements = additionalEntitlements;
      setFields.add("additionalEntitlements");
      return this;
    }

    public UpdateLicenseRequest build() {
      return new UpdateLicenseRequest(this);
    }
  }
}
