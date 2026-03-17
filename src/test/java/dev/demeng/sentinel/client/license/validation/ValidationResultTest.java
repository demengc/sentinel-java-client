package dev.demeng.sentinel.client.license.validation;

import static org.junit.jupiter.api.Assertions.*;

import dev.demeng.sentinel.client.exception.LicenseValidationException;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ValidationResultTest {

  private static final ValidationDetails SAMPLE_DETAILS =
      new ValidationDetails(
          Instant.parse("2026-12-31T23:59:59Z"), 1, 3, 1, 3, "Pro", Set.of("advanced-features"));

  @Test
  void successResultIsValid() {
    ValidationResult result = ValidationResult.success(SAMPLE_DETAILS, "License validated.");
    assertTrue(result.isValid());
    assertEquals(ValidationResultType.SUCCESS, result.getType());
    assertEquals("License validated.", result.getMessage());
    assertSame(SAMPLE_DETAILS, result.getDetails());
    assertNull(result.getFailureDetails());
  }

  @Test
  void failedResultIsNotValid() {
    ValidationResult result =
        ValidationResult.failure(ValidationResultType.EXPIRED_LICENSE, "License expired.");
    assertFalse(result.isValid());
    assertEquals(ValidationResultType.EXPIRED_LICENSE, result.getType());
    assertEquals("License expired.", result.getMessage());
    assertNull(result.getDetails());
    assertNull(result.getFailureDetails());
  }

  @Test
  void failedResultWithFailureDetails() {
    FailureDetails details = new FailureDetails.ExcessiveServersDetails(3);
    ValidationResult result =
        ValidationResult.failure(
            ValidationResultType.EXCESSIVE_SERVERS, "Too many servers.", details);
    assertFalse(result.isValid());
    assertSame(details, result.getFailureDetails());
  }

  @Test
  void requireValidReturnsDetailsOnSuccess() throws LicenseValidationException {
    ValidationResult result = ValidationResult.success(SAMPLE_DETAILS, "OK");
    assertSame(SAMPLE_DETAILS, result.requireValid());
  }

  @Test
  void requireValidThrowsOnFailure() {
    ValidationResult result =
        ValidationResult.failure(ValidationResultType.INVALID_LICENSE, "Bad license.");
    LicenseValidationException ex =
        assertThrows(LicenseValidationException.class, result::requireValid);
    assertEquals(ValidationResultType.INVALID_LICENSE, ex.getType());
    assertEquals("Bad license.", ex.getMessage());
  }
}
