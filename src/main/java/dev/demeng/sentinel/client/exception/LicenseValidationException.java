package dev.demeng.sentinel.client.exception;

import dev.demeng.sentinel.client.validation.ValidationResultType;

/**
 * Thrown by {@link dev.demeng.sentinel.client.validation.ValidationResult#requireValid()} when the
 * license validation result indicates failure.
 */
public final class LicenseValidationException extends SentinelException {

  private final ValidationResultType type;

  /**
   * Creates a new license validation exception.
   *
   * @param type the validation failure type
   * @param message the failure message from the API
   */
  public LicenseValidationException(ValidationResultType type, String message) {
    super(message);
    this.type = type;
  }

  /**
   * Returns the validation failure type.
   *
   * @return the failure type
   */
  public ValidationResultType getType() {
    return type;
  }
}
