package dev.demeng.sentinel.client.exception;

import java.io.IOException;

/**
 * Thrown when the client cannot connect to the Sentinel API, for example due to a network timeout
 * or DNS resolution failure.
 */
public final class SentinelConnectionException extends SentinelException {

  public SentinelConnectionException(String message, IOException cause) {
    super(message, cause);
  }

  @Override
  public IOException getCause() {
    return (IOException) super.getCause();
  }
}
