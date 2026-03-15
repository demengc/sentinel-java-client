package dev.demeng.sentinel.client.exception;

/**
 * Thrown when a validation response nonce has already been seen, indicating a potential replay
 * attack.
 */
public final class ReplayDetectedException extends SentinelException {

  public ReplayDetectedException(String message) {
    super(message);
  }
}
