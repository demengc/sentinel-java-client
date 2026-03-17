package dev.demeng.sentinel.client.license.validation;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ValidationRequestTest {

  @Test
  void buildsWithAllFields() {
    ValidationRequest request =
        ValidationRequest.builder()
            .key("ABC-12345-XYZ")
            .product("my-product")
            .server("server-1")
            .ip("1.2.3.4")
            .connectionPlatform("discord")
            .connectionValue("123456789")
            .build();

    assertEquals("ABC-12345-XYZ", request.getKey());
    assertEquals("my-product", request.getProduct());
    assertEquals("server-1", request.getServer());
    assertEquals("1.2.3.4", request.getIp());
    assertEquals("discord", request.getConnectionPlatform());
    assertEquals("123456789", request.getConnectionValue());
  }

  @Test
  void buildsWithOnlyRequiredFields() {
    ValidationRequest request =
        ValidationRequest.builder().product("my-product").server("server-1").build();

    assertNull(request.getKey());
    assertEquals("my-product", request.getProduct());
    assertEquals("server-1", request.getServer());
    assertNull(request.getIp());
    assertNull(request.getConnectionPlatform());
    assertNull(request.getConnectionValue());
  }

  @Test
  void throwsWhenProductMissing() {
    IllegalStateException ex =
        assertThrows(
            IllegalStateException.class,
            () -> ValidationRequest.builder().server("server-1").build());
    assertTrue(ex.getMessage().contains("product"));
  }

  @Test
  void throwsWhenServerMissing() {
    IllegalStateException ex =
        assertThrows(
            IllegalStateException.class,
            () -> ValidationRequest.builder().product("my-product").build());
    assertTrue(ex.getMessage().contains("server"));
  }
}
