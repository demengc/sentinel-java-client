package dev.demeng.sentinel.client.license;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jspecify.annotations.Nullable;

public record License(
    String id,
    String key,
    LicenseProduct product,
    LicenseTier tier,
    LicenseIssuer issuer,
    Instant createdAt,
    @Nullable Instant expiration,
    int maxServers,
    int maxIps,
    @Nullable String note,
    Map<String, String> connections,
    List<SubUser> subUsers,
    Map<String, Instant> servers,
    Map<String, Instant> ips,
    Set<String> additionalEntitlements,
    Set<String> entitlements,
    @Nullable BlacklistInfo blacklist) {

  public License {
    connections = connections == null ? Map.of() : Map.copyOf(connections);
    subUsers = subUsers == null ? List.of() : List.copyOf(subUsers);
    servers = servers == null ? Map.of() : Map.copyOf(servers);
    ips = ips == null ? Map.of() : Map.copyOf(ips);
    additionalEntitlements =
        additionalEntitlements == null ? Set.of() : Set.copyOf(additionalEntitlements);
    entitlements = entitlements == null ? Set.of() : Set.copyOf(entitlements);
  }
}
