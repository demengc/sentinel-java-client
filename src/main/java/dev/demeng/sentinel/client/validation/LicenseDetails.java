package dev.demeng.sentinel.client.validation;

import java.time.Instant;
import java.util.Set;

/**
 * Details of a successfully validated license.
 *
 * @param expiration the license expiration timestamp, or {@code null} if the license does not
 *     expire
 * @param serverCount the current number of servers using this license
 * @param maxServers the maximum allowed servers ({@code -1} for unlimited)
 * @param ipCount the current number of IP addresses using this license
 * @param maxIps the maximum allowed IP addresses ({@code -1} for unlimited)
 * @param tier the license tier name, or {@code null} if not set
 * @param entitlements the set of entitlement identifiers granted by this license (never {@code
 *     null})
 */
public record LicenseDetails(
    Instant expiration,
    int serverCount,
    int maxServers,
    int ipCount,
    int maxIps,
    String tier,
    Set<String> entitlements) {

  public LicenseDetails {
    entitlements = entitlements == null ? Set.of() : Set.copyOf(entitlements);
  }
}
