package dev.demeng.sentinel.client.internal;

import static org.junit.jupiter.api.Assertions.*;

import dev.demeng.sentinel.client.exception.ReplayDetectedException;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class ReplayProtectorTest {

  @Test
  void acceptsFreshNonceAndTimestamp() {
    ReplayProtector protector = new ReplayProtector(Duration.ofSeconds(30), 100);
    assertDoesNotThrow(() -> protector.check("nonce-1", System.currentTimeMillis()));
  }

  @Test
  void rejectsDuplicateNonce() throws ReplayDetectedException {
    ReplayProtector protector = new ReplayProtector(Duration.ofSeconds(30), 100);
    long now = System.currentTimeMillis();
    protector.check("nonce-1", now);
    assertThrows(ReplayDetectedException.class, () -> protector.check("nonce-1", now + 1));
  }

  @Test
  void rejectsTimestampTooFarInPast() {
    ReplayProtector protector = new ReplayProtector(Duration.ofSeconds(30), 100);
    long stale = System.currentTimeMillis() - 60_000;
    assertThrows(ReplayDetectedException.class, () -> protector.check("nonce-1", stale));
  }

  @Test
  void rejectsTimestampTooFarInFuture() {
    ReplayProtector protector = new ReplayProtector(Duration.ofSeconds(30), 100);
    long future = System.currentTimeMillis() + 60_000;
    assertThrows(ReplayDetectedException.class, () -> protector.check("nonce-1", future));
  }

  @Test
  void lruEvictsOldestNonce() throws ReplayDetectedException {
    ReplayProtector protector = new ReplayProtector(Duration.ofSeconds(30), 2);
    long now = System.currentTimeMillis();
    protector.check("nonce-1", now);
    protector.check("nonce-2", now);
    protector.check("nonce-3", now);
    // nonce-1 should have been evicted
    assertDoesNotThrow(() -> protector.check("nonce-1", now));
  }
}
