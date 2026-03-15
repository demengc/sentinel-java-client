package dev.demeng.sentinel.client.exception;

/**
 * Thrown when the Sentinel API returns an error response (e.g. 401 Unauthorized, 429 Too Many
 * Requests, or 500 Internal Server Error).
 *
 * <p>This exception is not thrown for 403 validation failures, which are instead returned as a
 * failed {@link dev.demeng.sentinel.client.validation.ValidationResult}.
 */
public final class SentinelApiException extends SentinelException {

  private final int httpStatus;
  private final String type;
  private final int retryAfterSeconds;

  /**
   * Creates a new API exception with no retry-after or cause.
   *
   * @param httpStatus the HTTP status code
   * @param type the error type from the API response
   * @param message the error message from the API response
   */
  public SentinelApiException(int httpStatus, String type, String message) {
    this(httpStatus, type, message, -1, null);
  }

  /**
   * Creates a new API exception with a retry-after value.
   *
   * @param httpStatus the HTTP status code
   * @param type the error type from the API response
   * @param message the error message from the API response
   * @param retryAfterSeconds seconds to wait before retrying, or {@code -1} if not specified
   */
  public SentinelApiException(int httpStatus, String type, String message, int retryAfterSeconds) {
    this(httpStatus, type, message, retryAfterSeconds, null);
  }

  /**
   * Creates a new API exception with a cause.
   *
   * @param httpStatus the HTTP status code
   * @param type the error type from the API response
   * @param message the error message from the API response
   * @param cause the underlying exception
   */
  public SentinelApiException(int httpStatus, String type, String message, Throwable cause) {
    this(httpStatus, type, message, -1, cause);
  }

  /**
   * Creates a new API exception with all fields.
   *
   * @param httpStatus the HTTP status code
   * @param type the error type from the API response
   * @param message the error message from the API response
   * @param retryAfterSeconds seconds to wait before retrying, or {@code -1} if not specified
   * @param cause the underlying exception, or {@code null}
   */
  public SentinelApiException(
      int httpStatus, String type, String message, int retryAfterSeconds, Throwable cause) {
    super(message, cause);
    this.httpStatus = httpStatus;
    this.type = type;
    this.retryAfterSeconds = retryAfterSeconds;
  }

  /**
   * Returns the HTTP status code of the error response.
   *
   * @return the HTTP status code
   */
  public int getHttpStatus() {
    return httpStatus;
  }

  /**
   * Returns the error type from the API response (e.g. {@code "UNAUTHORIZED"}).
   *
   * @return the error type, or {@code null}
   */
  public String getType() {
    return type;
  }

  /**
   * Returns the number of seconds to wait before retrying, or {@code -1} if the server did not
   * include a Retry-After header.
   *
   * @return the retry-after value in seconds, or {@code -1}
   */
  public int getRetryAfterSeconds() {
    return retryAfterSeconds;
  }
}
