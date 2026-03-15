package dev.demeng.sentinel.client.validation;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ValidationResultTypeTest {

  @Test
  void fromStringMapsKnownTypes() {
    assertEquals(ValidationResultType.SUCCESS, ValidationResultType.fromString("SUCCESS"));
    assertEquals(
        ValidationResultType.INVALID_PRODUCT, ValidationResultType.fromString("INVALID_PRODUCT"));
    assertEquals(
        ValidationResultType.INVALID_LICENSE, ValidationResultType.fromString("INVALID_LICENSE"));
    assertEquals(
        ValidationResultType.INVALID_PLATFORM, ValidationResultType.fromString("INVALID_PLATFORM"));
    assertEquals(
        ValidationResultType.EXPIRED_LICENSE, ValidationResultType.fromString("EXPIRED_LICENSE"));
    assertEquals(
        ValidationResultType.BLACKLISTED_LICENSE,
        ValidationResultType.fromString("BLACKLISTED_LICENSE"));
    assertEquals(
        ValidationResultType.CONNECTION_MISMATCH,
        ValidationResultType.fromString("CONNECTION_MISMATCH"));
    assertEquals(
        ValidationResultType.EXCESSIVE_SERVERS,
        ValidationResultType.fromString("EXCESSIVE_SERVERS"));
    assertEquals(
        ValidationResultType.EXCESSIVE_IPS, ValidationResultType.fromString("EXCESSIVE_IPS"));
  }

  @Test
  void fromStringReturnsUnknownForUnrecognizedType() {
    assertEquals(ValidationResultType.UNKNOWN, ValidationResultType.fromString("SOMETHING_NEW"));
    assertEquals(ValidationResultType.UNKNOWN, ValidationResultType.fromString(""));
    assertEquals(ValidationResultType.UNKNOWN, ValidationResultType.fromString(null));
  }
}
