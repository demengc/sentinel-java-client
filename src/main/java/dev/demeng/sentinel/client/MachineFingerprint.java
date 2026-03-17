package dev.demeng.sentinel.client;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;

/**
 * Generates a stable, unique fingerprint for the current machine.
 *
 * <p>This is intended to be used as the {@code server} value in a {@link
 * dev.demeng.sentinel.client.license.validation.ValidationRequest}, allowing Sentinel to track
 * which machine instances are using a license.
 *
 * <p>The fingerprint is derived from platform-specific machine identifiers:
 *
 * <ul>
 *   <li><b>Linux</b>: {@code /etc/machine-id} (persistent per OS installation)
 *   <li><b>macOS</b>: {@code IOPlatformUUID} (hardware UUID)
 *   <li><b>Windows</b>: {@code MachineGuid} from the registry (persistent per OS installation)
 * </ul>
 *
 * <p>If the platform-specific identifier cannot be read, a fallback based on system properties,
 * hostname, all non-loopback MAC addresses, and container/DMI identifiers is used. Note that {@code
 * /sys/class/dmi/id/product_uuid} typically requires root and will silently be skipped for
 * unprivileged processes.
 *
 * <p>The result is always a 32-character lowercase hex string (SHA-256 truncated to 128 bits).
 */
public final class MachineFingerprint {

  private static final int PROCESS_TIMEOUT_SECONDS = 5;
  private static volatile String cached;

  private MachineFingerprint() {}

  /**
   * Generates a fingerprint for the current machine.
   *
   * @return a 32-character lowercase hex string
   */
  public static String generate() {
    String result = cached;
    if (result != null) {
      return result;
    }
    result = sha256Hex(getPlatformId()).substring(0, 32);
    cached = result;
    return result;
  }

  private static String getPlatformId() {
    String os = System.getProperty("os.name", "").toLowerCase();

    if (os.contains("linux")) {
      String id = readLinuxMachineId();
      if (id != null) return id;
    } else if (os.contains("mac") || os.contains("darwin")) {
      String id = readMacUuid();
      if (id != null) return id;
    } else if (os.contains("win")) {
      String id = readWindowsMachineGuid();
      if (id != null) return id;
    }

    return fallbackFingerprint();
  }

  private static @Nullable String readLinuxMachineId() {
    try {
      return Files.readString(Path.of("/etc/machine-id")).trim();
    } catch (Exception ignored) {
    }

    try {
      return Files.readString(Path.of("/var/lib/dbus/machine-id")).trim();
    } catch (Exception ignored) {
    }

    return null;
  }

  private static @Nullable String readMacUuid() {
    try {
      Process process =
          new ProcessBuilder("ioreg", "-rd1", "-c", "IOPlatformExpertDevice")
              .redirectErrorStream(true)
              .start();
      String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
      if (!process.waitFor(PROCESS_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
        process.destroyForcibly();
        return null;
      }

      for (String line : output.split("\n")) {
        if (line.contains("IOPlatformUUID")) {
          int eqIndex = line.indexOf('=');
          if (eqIndex < 0) continue;
          int start = line.indexOf('"', eqIndex);
          int end = line.indexOf('"', start + 1);
          if (start >= 0 && end > start) {
            return line.substring(start + 1, end);
          }
        }
      }
    } catch (Exception ignored) {
    }

    return null;
  }

  private static @Nullable String readWindowsMachineGuid() {
    try {
      Process process =
          new ProcessBuilder(
                  "reg", "query", "HKLM\\SOFTWARE\\Microsoft\\Cryptography", "/v", "MachineGuid")
              .redirectErrorStream(true)
              .start();
      String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
      if (!process.waitFor(PROCESS_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
        process.destroyForcibly();
        return null;
      }

      for (String line : output.split("\n")) {
        if (line.contains("MachineGuid")) {
          String[] parts = line.trim().split("\\s+");
          if (parts.length >= 3) {
            return parts[parts.length - 1].trim();
          }
        }
      }
    } catch (Exception ignored) {
    }

    return null;
  }

  private static String fallbackFingerprint() {
    StringBuilder sb = new StringBuilder();
    sb.append(System.getProperty("os.name", ""));
    sb.append(System.getProperty("os.arch", ""));
    sb.append(System.getProperty("user.name", ""));
    sb.append(System.getProperty("user.home", ""));

    try {
      sb.append(InetAddress.getLocalHost().getHostName());
    } catch (Exception ignored) {
    }

    try {
      String dmiUuid = Files.readString(Path.of("/sys/class/dmi/id/product_uuid")).trim();
      sb.append(dmiUuid);
    } catch (Exception ignored) {
    }

    String containerId = readContainerId();
    if (containerId != null) {
      sb.append(containerId);
    }

    try {
      Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
      if (interfaces != null) {
        List<String> macs = new ArrayList<>();
        while (interfaces.hasMoreElements()) {
          NetworkInterface ni = interfaces.nextElement();
          if (!ni.isLoopback() && !ni.isVirtual() && ni.getHardwareAddress() != null) {
            byte[] mac = ni.getHardwareAddress();
            StringBuilder macStr = new StringBuilder(mac.length * 2);
            for (byte b : mac) {
              macStr.append(String.format("%02x", b & 0xFF));
            }
            macs.add(macStr.toString());
          }
        }
        Collections.sort(macs);
        for (String mac : macs) {
          sb.append(mac);
        }
      }
    } catch (Exception ignored) {
    }

    return sb.toString();
  }

  private static @Nullable String readContainerId() {
    try {
      String content = Files.readString(Path.of("/proc/self/mountinfo"));
      return extractContainerId(content);
    } catch (Exception ignored) {
    }

    try {
      String content = Files.readString(Path.of("/proc/self/cgroup"));
      return extractContainerId(content);
    } catch (Exception ignored) {
    }

    return null;
  }

  private static @Nullable String extractContainerId(String content) {
    Matcher matcher = Pattern.compile("[a-f0-9]{64}").matcher(content);
    if (matcher.find()) {
      return matcher.group();
    }
    return null;
  }

  private static String sha256Hex(String input) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
      StringBuilder hex = new StringBuilder(hash.length * 2);
      for (byte b : hash) {
        hex.append(String.format("%02x", b & 0xFF));
      }
      return hex.toString();
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("SHA-256 not available", e);
    }
  }
}
