package dev.demeng.sentinel.client.validation;

/**
 * The type of a license validation result, indicating whether validation succeeded and, if not, the
 * specific reason for failure.
 */
public enum ValidationResultType {
  /** The license is valid. */
  SUCCESS,
  /**
   * The product validation failed. Check {@link ValidationResult#getMessage()} to distinguish
   * between: product not found, license belonging to a different product, or auto-creation not
   * enabled for the product.
   */
  INVALID_PRODUCT,
  /** The license key is not recognized. */
  INVALID_LICENSE,
  /** Auto-creation was attempted but the request is missing a connection platform or value. */
  INVALID_PLATFORM,
  /** The license has expired. */
  EXPIRED_LICENSE,
  /** The license has been blacklisted. */
  BLACKLISTED_LICENSE,
  /** The connection value does not match the license's registered connection. */
  CONNECTION_MISMATCH,
  /** The license has exceeded its maximum number of concurrent servers. */
  EXCESSIVE_SERVERS,
  /** The license has exceeded its maximum number of concurrent IP addresses. */
  EXCESSIVE_IPS,
  /** An unrecognized validation result type returned by the API. */
  UNKNOWN;

  /**
   * Converts a string value to a {@code ValidationResultType}, returning {@link #UNKNOWN} if the
   * value is {@code null} or unrecognized.
   *
   * @param value the string representation of the result type
   * @return the matching enum constant, or {@link #UNKNOWN}
   */
  public static ValidationResultType fromString(String value) {
    if (value == null) {
      return UNKNOWN;
    }
    try {
      return valueOf(value);
    } catch (IllegalArgumentException e) {
      return UNKNOWN;
    }
  }
}
