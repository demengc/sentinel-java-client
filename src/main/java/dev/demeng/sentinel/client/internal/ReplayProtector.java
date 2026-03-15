package dev.demeng.sentinel.client.internal;

import dev.demeng.sentinel.client.exception.ReplayDetectedException;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ReplayProtector {

  private final Duration window;
  private final Map<String, Boolean> nonceCache;

  public ReplayProtector(Duration window, int maxSize) {
    this.window = window;
    this.nonceCache =
        new LinkedHashMap<>(maxSize, 0.75f, false) {
          @Override
          protected boolean removeEldestEntry(Map.Entry<String, Boolean> eldest) {
            return size() > maxSize;
          }
        };
  }

  public synchronized void check(String nonce, long responseTimestamp)
      throws ReplayDetectedException {
    long now = System.currentTimeMillis();
    long drift = Math.abs(now - responseTimestamp);

    if (drift > window.toMillis()) {
      throw new ReplayDetectedException(
          "Response timestamp is outside the acceptable window: drift="
              + drift
              + "ms, window="
              + window.toMillis()
              + "ms");
    }

    if (nonceCache.putIfAbsent(nonce, Boolean.TRUE) != null) {
      throw new ReplayDetectedException("Duplicate nonce detected: " + nonce);
    }
  }
}
