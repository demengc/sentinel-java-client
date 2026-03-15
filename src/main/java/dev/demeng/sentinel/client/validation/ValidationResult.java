package dev.demeng.sentinel.client.validation;

import dev.demeng.sentinel.client.exception.LicenseValidationException;

/**
 * The result of a license validation request.
 *
 * <p>Check {@link #isValid()} to determine whether the license is valid. On success, {@link
 * #getDetails()} provides the license details. On failure, {@link #getType()} and {@link
 * #getMessage()} describe the reason, and {@link #getFailureDetails()} may provide additional
 * context for certain failure types.
 *
 * <p>Alternatively, use {@link #requireValid()} to throw a {@link LicenseValidationException} if
 * the license is not valid.
 */
public final class ValidationResult {

  private final ValidationResultType type;
  private final String message;
  private final LicenseDetails details;
  private final FailureDetails failureDetails;

  private ValidationResult(
      ValidationResultType type,
      String message,
      LicenseDetails details,
      FailureDetails failureDetails) {
    this.type = type;
    this.message = message;
    this.details = details;
    this.failureDetails = failureDetails;
  }

  /**
   * Creates a successful validation result.
   *
   * @param details the license details
   * @param message the API response message
   * @return a successful result
   */
  public static ValidationResult success(LicenseDetails details, String message) {
    return new ValidationResult(ValidationResultType.SUCCESS, message, details, null);
  }

  /**
   * Creates a failed validation result with no additional failure details.
   *
   * @param type the failure type
   * @param message the API response message
   * @return a failed result
   */
  public static ValidationResult failure(ValidationResultType type, String message) {
    return new ValidationResult(type, message, null, null);
  }

  /**
   * Creates a failed validation result with additional failure details.
   *
   * @param type the failure type
   * @param message the API response message
   * @param failureDetails additional context about the failure, or {@code null}
   * @return a failed result
   */
  public static ValidationResult failure(
      ValidationResultType type, String message, FailureDetails failureDetails) {
    return new ValidationResult(type, message, null, failureDetails);
  }

  /**
   * Returns {@code true} if the license is valid.
   *
   * @return whether the license passed validation
   */
  public boolean isValid() {
    return type == ValidationResultType.SUCCESS;
  }

  /**
   * Returns the validation result type.
   *
   * @return the result type (e.g. {@link ValidationResultType#SUCCESS}, {@link
   *     ValidationResultType#EXPIRED_LICENSE})
   */
  public ValidationResultType getType() {
    return type;
  }

  /**
   * Returns the message from the API response.
   *
   * @return the response message
   */
  public String getMessage() {
    return message;
  }

  /**
   * Returns the license details if the validation was successful, or {@code null} on failure.
   *
   * @return the license details, or {@code null}
   */
  public LicenseDetails getDetails() {
    return details;
  }

  /**
   * Returns additional details about the failure, or {@code null} if unavailable. Only populated
   * for certain failure types such as {@link ValidationResultType#BLACKLISTED_LICENSE}, {@link
   * ValidationResultType#EXCESSIVE_SERVERS}, and {@link ValidationResultType#EXCESSIVE_IPS}.
   *
   * @return the failure details, or {@code null}
   */
  public FailureDetails getFailureDetails() {
    return failureDetails;
  }

  /**
   * Returns the license details if valid, or throws a {@link LicenseValidationException} if the
   * license failed validation.
   *
   * @return the license details
   * @throws LicenseValidationException if the license is not valid
   */
  public LicenseDetails requireValid() throws LicenseValidationException {
    if (!isValid()) {
      throw new LicenseValidationException(type, message);
    }
    return details;
  }

  @Override
  public String toString() {
    return "ValidationResult{type="
        + type
        + ", message='"
        + message
        + "'"
        + (details != null ? ", details=" + details : "")
        + (failureDetails != null ? ", failureDetails=" + failureDetails : "")
        + "}";
  }
}
