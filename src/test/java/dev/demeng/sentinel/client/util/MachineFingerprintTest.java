package dev.demeng.sentinel.client.util;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class MachineFingerprintTest {

  @Test
  void generateReturns32CharHexString() {
    String fingerprint = MachineFingerprint.generate();
    assertTrue(fingerprint.matches("[0-9a-f]{32}"), "Expected 32-char hex, got: " + fingerprint);
  }

  @Test
  void generateIsStableAcrossCalls() {
    assertEquals(MachineFingerprint.generate(), MachineFingerprint.generate());
  }
}
