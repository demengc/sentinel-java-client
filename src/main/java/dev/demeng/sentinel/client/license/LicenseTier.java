package dev.demeng.sentinel.client.license;

import java.util.Set;

public record LicenseTier(String name, Set<String> entitlements) {
  public LicenseTier {
    entitlements = entitlements == null ? Set.of() : Set.copyOf(entitlements);
  }
}
