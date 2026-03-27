package dev.demeng.sentinel.client.util;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class PublicIpTest {

  @Test
  void resolveReturnsValidIpAddress() {
    String ip = PublicIp.resolve();
    assertNotNull(ip, "Expected a public IP but got null");
    assertTrue(
        ip.matches("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}"),
        "Expected IPv4 address, got: " + ip);
  }

  @Test
  void resolveIsStableAcrossCalls() {
    assertEquals(PublicIp.resolve(), PublicIp.resolve());
  }
}
