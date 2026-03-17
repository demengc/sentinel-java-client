package dev.demeng.sentinel.client.license;

import java.util.Set;
import org.jspecify.annotations.Nullable;

public record LicenseTier(String id, @Nullable String name, Set<String> entitlements) {
  public LicenseTier {
    entitlements = entitlements == null ? Set.of() : Set.copyOf(entitlements);
  }
}
