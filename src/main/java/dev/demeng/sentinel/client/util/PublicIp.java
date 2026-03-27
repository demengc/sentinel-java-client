package dev.demeng.sentinel.client.util;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.jspecify.annotations.Nullable;

/**
 * Resolves the public IP address of the current machine by querying an external service.
 *
 * <p>This is useful as a workaround when the Sentinel server sits behind a reverse proxy that does
 * not forward the real client IP (e.g. missing {@code X-Forwarded-For} headers). The resolved
 * address can be passed to {@link
 * dev.demeng.sentinel.client.license.validation.ValidationRequest.Builder#ip(String)} so that
 * Sentinel sees the correct IP regardless of proxy configuration.
 *
 * <pre>{@code
 * ValidationRequest request = ValidationRequest.builder()
 *     .product("my-product")
 *     .ip(PublicIp.resolve())
 *     .build();
 * }</pre>
 */
public final class PublicIp {

  private static final String CHECKIP_URL = "https://checkip.amazonaws.com";
  private static final int TIMEOUT_SECONDS = 5;

  private PublicIp() {}

  /**
   * Resolves the public IP address of the current machine.
   *
   * @return the public IP address, or {@code null} if the lookup fails for any reason
   */
  public static @Nullable String resolve() {
    try {
      Duration timeout = Duration.ofSeconds(TIMEOUT_SECONDS);
      HttpClient client = HttpClient.newBuilder().connectTimeout(timeout).build();
      HttpRequest request =
          HttpRequest.newBuilder().uri(URI.create(CHECKIP_URL)).timeout(timeout).GET().build();
      HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() != 200) {
        return null;
      }
      String ip = response.body().strip();
      return ip.isEmpty() ? null : ip;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return null;
    } catch (Exception e) {
      return null;
    }
  }
}
