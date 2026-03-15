package dev.demeng.sentinel.client.exception;

/**
 * Base exception for all errors thrown by the Sentinel client. This is a sealed class; all possible
 * subtypes are known at compile time.
 */
public sealed class SentinelException extends Exception
    permits SentinelApiException,
        SentinelConnectionException,
        LicenseValidationException,
        SignatureVerificationException,
        ReplayDetectedException {

  public SentinelException(String message) {
    super(message);
  }

  public SentinelException(String message, Throwable cause) {
    super(message, cause);
  }
}
