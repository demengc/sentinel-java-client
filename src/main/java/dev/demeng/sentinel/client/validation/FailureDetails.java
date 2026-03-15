package dev.demeng.sentinel.client.validation;

import java.time.Instant;

/**
 * Additional details returned by the API for certain validation failure types. Obtained from {@link
 * ValidationResult#getFailureDetails()}.
 *
 * @see ValidationResult#getFailureDetails()
 */
public sealed interface FailureDetails {

  /**
   * Details for a {@link ValidationResultType#BLACKLISTED_LICENSE} failure.
   *
   * @param timestamp when the license was blacklisted
   * @param reason the reason for blacklisting, or {@code null} if not provided
   */
  record BlacklistDetails(Instant timestamp, String reason) implements FailureDetails {}

  /**
   * Details for a {@link ValidationResultType#EXCESSIVE_SERVERS} failure.
   *
   * @param maxServers the maximum number of servers allowed by the license
   */
  record ExcessiveServersDetails(int maxServers) implements FailureDetails {}

  /**
   * Details for a {@link ValidationResultType#EXCESSIVE_IPS} failure.
   *
   * @param maxIps the maximum number of IP addresses allowed by the license
   */
  record ExcessiveIpsDetails(int maxIps) implements FailureDetails {}
}
