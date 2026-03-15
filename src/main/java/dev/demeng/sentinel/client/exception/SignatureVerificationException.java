package dev.demeng.sentinel.client.exception;

/**
 * Thrown when the cryptographic signature on a validation response does not match the response
 * payload, indicating the response may have been tampered with.
 */
public final class SignatureVerificationException extends SentinelException {

  public SignatureVerificationException(String message) {
    super(message);
  }

  public SignatureVerificationException(String message, Throwable cause) {
    super(message, cause);
  }
}
