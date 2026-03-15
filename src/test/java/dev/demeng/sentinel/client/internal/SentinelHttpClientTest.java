package dev.demeng.sentinel.client.internal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import dev.demeng.sentinel.client.exception.SentinelConnectionException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;

@SuppressWarnings("unchecked")
class SentinelHttpClientTest {

  private SentinelHttpClient createClient(HttpClient mockJavaClient) {
    return new SentinelHttpClient(
        mockJavaClient, "https://api.example.com", "test-key", Duration.ofSeconds(5));
  }

  private HttpResponse<String> mockResponse(int status, String body) {
    HttpResponse<String> response = mock(HttpResponse.class);
    when(response.statusCode()).thenReturn(status);
    when(response.body()).thenReturn(body);
    var headers = mock(java.net.http.HttpHeaders.class);
    when(headers.map()).thenReturn(Map.of());
    when(response.headers()).thenReturn(headers);
    return response;
  }

  @Test
  void postRequestSetsMethodAndBody() throws Exception {
    HttpClient mockJavaClient = mock(HttpClient.class);
    HttpResponse<String> mockResp = mockResponse(200, "{\"ok\":true}");
    when(mockJavaClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(mockResp);

    SentinelHttpClient client = createClient(mockJavaClient);
    SentinelHttpResponse result =
        client.request("POST", "/api/v2/licenses/validate", "{\"product\":\"p\"}");

    assertEquals(200, result.statusCode());
    assertEquals("{\"ok\":true}", result.body());

    var captor = org.mockito.ArgumentCaptor.forClass(HttpRequest.class);
    verify(mockJavaClient).send(captor.capture(), any());
    HttpRequest captured = captor.getValue();
    assertEquals("POST", captured.method());
    assertEquals("https://api.example.com/api/v2/licenses/validate", captured.uri().toString());
    assertTrue(captured.headers().firstValue("Authorization").orElse("").contains("test-key"));
    assertEquals("application/json", captured.headers().firstValue("Content-Type").orElse(""));
  }

  @Test
  void getRequestOmitsContentType() throws Exception {
    HttpClient mockJavaClient = mock(HttpClient.class);
    HttpResponse<String> mockResp = mockResponse(200, "{}");
    when(mockJavaClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(mockResp);

    SentinelHttpClient client = createClient(mockJavaClient);
    client.request("GET", "/api/v2/products");

    var captor = org.mockito.ArgumentCaptor.forClass(HttpRequest.class);
    verify(mockJavaClient).send(captor.capture(), any());
    HttpRequest captured = captor.getValue();
    assertEquals("GET", captured.method());
    assertTrue(captured.headers().firstValue("Content-Type").isEmpty());
  }

  @Test
  void queryParamsAppendedToUrl() throws Exception {
    HttpClient mockJavaClient = mock(HttpClient.class);
    HttpResponse<String> mockResp = mockResponse(200, "{}");
    when(mockJavaClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(mockResp);

    SentinelHttpClient client = createClient(mockJavaClient);
    client.request("GET", "/api/v2/licenses", null, Map.of("page", "0", "size", "50"));

    var captor = org.mockito.ArgumentCaptor.forClass(HttpRequest.class);
    verify(mockJavaClient).send(captor.capture(), any());
    String uri = captor.getValue().uri().toString();
    assertTrue(uri.startsWith("https://api.example.com/api/v2/licenses?"));
    assertTrue(uri.contains("page=0"));
    assertTrue(uri.contains("size=50"));
  }

  @Test
  void ioExceptionWrappedInSentinelConnectionException() throws Exception {
    HttpClient mockJavaClient = mock(HttpClient.class);
    when(mockJavaClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenThrow(new java.io.IOException("connection refused"));

    SentinelHttpClient client = createClient(mockJavaClient);
    assertThrows(
        SentinelConnectionException.class, () -> client.request("GET", "/api/v2/products"));
  }

  @Test
  void interruptedExceptionSetsInterruptFlag() throws Exception {
    HttpClient mockJavaClient = mock(HttpClient.class);
    when(mockJavaClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenThrow(new InterruptedException("interrupted"));

    SentinelHttpClient client = createClient(mockJavaClient);
    assertThrows(
        SentinelConnectionException.class, () -> client.request("GET", "/api/v2/products"));
    assertTrue(Thread.currentThread().isInterrupted());
    Thread.interrupted(); // clear flag for other tests
  }

  @Test
  void trailingSlashOnBaseUrlNormalized() throws Exception {
    HttpClient mockJavaClient = mock(HttpClient.class);
    HttpResponse<String> mockResp = mockResponse(200, "{}");
    when(mockJavaClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(mockResp);

    SentinelHttpClient client =
        new SentinelHttpClient(
            mockJavaClient, "https://api.example.com/", "key", Duration.ofSeconds(5));
    client.request("GET", "/api/v2/products");

    var captor = org.mockito.ArgumentCaptor.forClass(HttpRequest.class);
    verify(mockJavaClient).send(captor.capture(), any());
    assertEquals("https://api.example.com/api/v2/products", captor.getValue().uri().toString());
  }
}
