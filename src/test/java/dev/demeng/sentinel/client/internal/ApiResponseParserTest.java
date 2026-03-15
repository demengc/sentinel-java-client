package dev.demeng.sentinel.client.internal;

import static org.junit.jupiter.api.Assertions.*;

import dev.demeng.sentinel.client.exception.SentinelApiException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ApiResponseParserTest {

  private final ApiResponseParser parser = new ApiResponseParser();

  private SentinelHttpResponse response(int status, String body) {
    return new SentinelHttpResponse(status, body, Map.of());
  }

  private SentinelHttpResponse responseWithHeaders(
      int status, String body, Map<String, List<String>> headers) {
    return new SentinelHttpResponse(status, body, headers);
  }

  @Test
  void parses200Envelope() throws SentinelApiException {
    String json =
        """
                {"timestamp": 100, "status": "OK", "type": "SUCCESS", "message": "Done.", "result": {"key": "val"}}
                """;
    ApiResponseParser.ApiResponse parsed = parser.parse(response(200, json));
    assertEquals(200, parsed.httpStatus());
    assertEquals("SUCCESS", parsed.type());
    assertEquals("Done.", parsed.message());
    assertNotNull(parsed.result());
    assertEquals("val", parsed.result().get("key").getAsString());
  }

  @Test
  void parses201Envelope() throws SentinelApiException {
    String json =
        """
                {"timestamp": 100, "status": "CREATED", "type": "SUCCESS", "message": "Created.", "result": {"id": "1"}}
                """;
    ApiResponseParser.ApiResponse parsed = parser.parse(response(201, json));
    assertEquals(201, parsed.httpStatus());
  }

  @Test
  void allows403WhenExplicitlyWhitelisted() throws SentinelApiException {
    String json =
        """
                {"timestamp": 100, "status": "FORBIDDEN", "type": "EXPIRED_LICENSE", "message": "Expired.", "result": {}}
                """;
    ApiResponseParser.ApiResponse parsed = parser.parse(response(403, json), 403);
    assertEquals(403, parsed.httpStatus());
    assertEquals("EXPIRED_LICENSE", parsed.type());
    assertEquals("Expired.", parsed.message());
  }

  @Test
  void throws403WhenNotWhitelisted() {
    String json =
        """
                {"timestamp": 100, "status": "FORBIDDEN", "type": "FORBIDDEN", "message": "Access denied.", "result": {}}
                """;
    SentinelApiException ex =
        assertThrows(SentinelApiException.class, () -> parser.parse(response(403, json)));
    assertEquals(403, ex.getHttpStatus());
    assertEquals("FORBIDDEN", ex.getType());
  }

  @Test
  void throws401() {
    String json =
        """
                {"timestamp": 100, "status": "UNAUTHORIZED", "type": "UNAUTHORIZED", "message": "Bad key.", "result": null}
                """;
    SentinelApiException ex =
        assertThrows(SentinelApiException.class, () -> parser.parse(response(401, json)));
    assertEquals(401, ex.getHttpStatus());
  }

  @Test
  void throws429WithRetryAfterHeader() {
    String json =
        """
                {"timestamp": 100, "status": "TOO_MANY_REQUESTS", "type": "RATE_LIMITED", "message": "Slow down.", "result": null}
                """;
    SentinelApiException ex =
        assertThrows(
            SentinelApiException.class,
            () ->
                parser.parse(
                    responseWithHeaders(
                        429, json, Map.of("X-Rate-Limit-Retry-After-Seconds", List.of("30")))));
    assertEquals(429, ex.getHttpStatus());
    assertEquals(30, ex.getRetryAfterSeconds());
  }

  @Test
  void throws500() {
    String json =
        """
                {"timestamp": 100, "status": "INTERNAL_SERVER_ERROR", "type": "INTERNAL_ERROR", "message": "Oops.", "result": null}
                """;
    SentinelApiException ex =
        assertThrows(SentinelApiException.class, () -> parser.parse(response(500, json)));
    assertEquals(500, ex.getHttpStatus());
  }

  @Test
  void throwsOnNullBody() {
    assertThrows(
        SentinelApiException.class,
        () -> parser.parse(new SentinelHttpResponse(200, null, Map.of())));
  }

  @Test
  void throwsOnMalformedJson() {
    SentinelApiException ex =
        assertThrows(SentinelApiException.class, () -> parser.parse(response(200, "not json")));
    assertNotNull(ex.getCause());
  }

  @Test
  void handlesNullResult() throws SentinelApiException {
    String json =
        """
                {"timestamp": 100, "status": "OK", "type": "SUCCESS", "message": "OK.", "result": null}
                """;
    ApiResponseParser.ApiResponse parsed = parser.parse(response(200, json));
    assertNull(parsed.result());
  }
}
