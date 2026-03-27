package dev.demeng.sentinel.client.util;

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
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.jspecify.annotations.Nullable;

/**
 * Generates a stable, unique fingerprint for the current machine.
 *
 * <p>This is intended to be used as the {@code server} value in a {@link
 * dev.demeng.sentinel.client.license.validation.ValidationRequest}, allowing Sentinel to track
 * which machine instances are using a license.
 *
 * <p>The fingerprint is resolved using the following strategy, in order:
 *
 * <ol>
 *   <li><b>Platform-specific identifier</b>:
 *       <ul>
 *         <li>Linux: {@code /etc/machine-id} (persistent per OS installation)
 *         <li>macOS: {@code IOPlatformUUID} (hardware UUID)
 *         <li>Windows: {@code MachineGuid} from the registry (persistent per OS installation)
 *         <li>FreeBSD: {@code kern.hostuuid} via sysctl
 *       </ul>
 *   <li><b>Fallback</b>: zero-configuration signals that remain available to unprivileged
 *       processes, prioritizing host-visible hardware or virtualization identifiers, then usable
 *       MAC addresses with OS properties, then OS properties with hostname.
 * </ol>
 *
 * <p>The result is always a 32-character lowercase hex string (SHA-256 truncated to 128 bits).
 *
 * <p><b>Limitation:</b> Cloned VMs may share the same fingerprint as the source machine until the
 * OS regenerates its platform identifier (e.g., {@code /etc/machine-id} on Linux). macOS and
 * Windows do not automatically regenerate their identifiers after cloning.
 */
public final class MachineFingerprint {

  private static final int PROCESS_TIMEOUT_SECONDS = 5;
  private static final List<String> CONTAINER_MARKERS =
      List.of("docker", "containerd", "kubepods", "podman", "libpod", "lxc", "machine.slice");
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
    synchronized (MachineFingerprint.class) {
      result = cached;
      if (result == null) {
        result = sha256Hex(getPlatformId()).substring(0, 32);
        cached = result;
      }
      return result;
    }
  }

  private static String getPlatformId() {
    String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);

    if (os.contains("linux")) {
      String id = readLinuxMachineId();
      if (id != null) return id;
    } else if (os.contains("mac") || os.contains("darwin")) {
      String id = readMacUuid();
      if (id != null) return id;
    } else if (os.contains("win")) {
      String id = readWindowsMachineGuid();
      if (id != null) return id;
    } else if (os.contains("bsd")) {
      String id = readBsdHostUuid();
      if (id != null) return id;
    }

    return fallbackFingerprint();
  }

  private static @Nullable String readLinuxMachineId() {
    String id = readTextFile("/etc/machine-id");
    if (id != null) {
      return id;
    }
    return readTextFile("/var/lib/dbus/machine-id");
  }

  private static @Nullable String readMacUuid() {
    String output = runCommand("ioreg", "-rd1", "-c", "IOPlatformExpertDevice");
    if (output == null) {
      return null;
    }

    for (String line : output.split("\n")) {
      if (line.contains("IOPlatformUUID")) {
        int eqIndex = line.indexOf('=');
        if (eqIndex < 0) continue;
        int start = line.indexOf('"', eqIndex);
        if (start < 0) continue;
        int end = line.indexOf('"', start + 1);
        if (end > start) {
          return normalizeIdentifier(line.substring(start + 1, end));
        }
      }
    }

    return null;
  }

  private static @Nullable String readWindowsMachineGuid() {
    String output =
        runCommand("reg", "query", "HKLM\\SOFTWARE\\Microsoft\\Cryptography", "/v", "MachineGuid");
    if (output == null) {
      return null;
    }

    for (String line : output.split("\n")) {
      if (line.contains("MachineGuid")) {
        String[] parts = line.trim().split("\\s+");
        if (parts.length >= 3) {
          return normalizeIdentifier(parts[parts.length - 1]);
        }
      }
    }

    return null;
  }

  private static @Nullable String readBsdHostUuid() {
    String output = runCommand("sysctl", "-n", "kern.hostuuid");
    if (output == null) {
      return null;
    }
    return normalizeIdentifier(output);
  }

  private static String fallbackFingerprint() {
    StringBuilder sb = new StringBuilder();

    String stableHostId = readStableHostId();
    if (stableHostId != null) {
      sb.append('\0').append(stableHostId);
    }

    String dmiComposite = readDmiComposite();
    if (dmiComposite != null) {
      sb.append('\0').append(dmiComposite);
    }

    sb.append('\0').append(System.getProperty("os.name", ""));
    sb.append('\0').append(System.getProperty("os.arch", ""));

    boolean containerized = isContainerized();

    String hostname = null;
    if (!containerized) {
      hostname = resolveHostname();
      if (hostname != null) {
        sb.append('\0').append(hostname);
      }
    }
    List<String> macs = readMacAddresses(containerized);
    if (macs.isEmpty() && containerized) {
      macs = readMacAddresses(false);
    }
    for (String mac : macs) {
      sb.append('\0').append(mac);
    }

    if (macs.isEmpty() && hostname == null) {
      sb.append('\0').append(System.getProperty("user.name", ""));
      sb.append('\0').append(System.getProperty("user.home", ""));
    }

    String result = sb.toString();
    if (result.isBlank()) {
      return "static-fallback"
          + '\0'
          + System.getProperty("java.vm.name", "")
          + '\0'
          + System.getProperty("os.version", "");
    }
    return result;
  }

  private static @Nullable String resolveHostname() {
    String hostname = System.getenv("HOSTNAME");
    if (hostname != null && !hostname.isBlank()) {
      return hostname;
    }
    hostname = System.getenv("COMPUTERNAME");
    if (hostname != null && !hostname.isBlank()) {
      return hostname;
    }
    hostname = readTextFile("/etc/hostname");
    if (hostname != null) {
      return hostname;
    }
    return null;
  }

  private static @Nullable String readStableHostId() {
    String id = readTextFile("/sys/class/dmi/id/product_serial");
    if (id != null) {
      return id;
    }

    id = readTextFile("/sys/devices/virtual/dmi/id/product_serial");
    if (id != null) {
      return id;
    }

    id = readTextFile("/sys/class/dmi/id/board_serial");
    if (id != null) {
      return id;
    }

    id = readTextFile("/sys/devices/virtual/dmi/id/board_serial");
    if (id != null) {
      return id;
    }

    id = readBinaryTextFile("/proc/device-tree/serial-number");
    if (id != null) {
      return id;
    }

    id = readTextFile("/sys/class/dmi/id/product_uuid");
    if (id != null) {
      return id;
    }

    id = readTextFile("/sys/devices/virtual/dmi/id/product_uuid");
    if (id != null) {
      return id;
    }

    return null;
  }

  private static @Nullable String readDmiComposite() {
    StringBuilder composite = new StringBuilder();
    appendIfPresent(composite, readTextFile("/sys/class/dmi/id/board_vendor"));
    appendIfPresent(composite, readTextFile("/sys/class/dmi/id/board_name"));
    appendIfPresent(composite, readTextFile("/sys/class/dmi/id/product_name"));
    appendIfPresent(composite, readTextFile("/sys/class/dmi/id/sys_vendor"));
    if (!composite.isEmpty()) {
      return composite.toString();
    }
    return null;
  }

  private static void appendIfPresent(StringBuilder sb, @Nullable String value) {
    if (value != null) {
      sb.append('\0').append(value);
    }
  }

  private static List<String> readMacAddresses(boolean containerized) {
    try {
      Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
      if (interfaces == null) {
        return List.of();
      }

      List<String> macs = new ArrayList<>();
      while (interfaces.hasMoreElements()) {
        NetworkInterface ni = interfaces.nextElement();
        if (ni.isLoopback() || ni.isVirtual() || isIgnoredInterface(ni.getName())) {
          continue;
        }

        byte[] mac = ni.getHardwareAddress();
        if (mac == null || mac.length == 0 || isAllZeros(mac) || isAllOnes(mac)) {
          continue;
        }
        if (containerized && isLocallyAdministered(mac)) {
          continue;
        }

        StringBuilder macStr = new StringBuilder(mac.length * 2);
        for (byte b : mac) {
          macStr.append(String.format("%02x", b & 0xFF));
        }
        macs.add(macStr.toString());
      }
      Collections.sort(macs);
      return macs;
    } catch (Exception ignored) {
    }
    return List.of();
  }

  private static boolean isContainerized() {
    if (Files.exists(Path.of("/.dockerenv")) || Files.exists(Path.of("/run/.containerenv"))) {
      return true;
    }

    if (System.getenv("KUBERNETES_SERVICE_HOST") != null
        || System.getenv("container") != null
        || System.getenv("DOTNET_RUNNING_IN_CONTAINER") != null) {
      return true;
    }

    String cgroup = readRawTextFile("/proc/1/cgroup");
    if (cgroup != null) {
      String lower = cgroup.toLowerCase(Locale.ROOT);
      for (String marker : CONTAINER_MARKERS) {
        if (lower.contains(marker)) {
          return true;
        }
      }
    }

    String selfCgroup = readRawTextFile("/proc/self/cgroup");
    if (selfCgroup != null) {
      for (String line : selfCgroup.split("\n")) {
        if (line.startsWith("0::")) {
          String path = line.substring(3).trim().toLowerCase(Locale.ROOT);
          for (String marker : CONTAINER_MARKERS) {
            if (path.contains(marker)) {
              return true;
            }
          }
        }
      }
    }

    String mountinfo = readRawTextFile("/proc/1/mountinfo");
    if (mountinfo != null) {
      for (String line : mountinfo.split("\n")) {
        if (line.contains(" / / ") && line.contains("overlay")) {
          return true;
        }
      }
    }

    return false;
  }

  private static boolean isIgnoredInterface(@Nullable String interfaceName) {
    if (interfaceName == null || interfaceName.isBlank()) {
      return true;
    }

    String lower = interfaceName.toLowerCase(Locale.ROOT);
    return lower.equals("lo")
        || lower.startsWith("docker")
        || lower.startsWith("br-")
        || lower.startsWith("veth")
        || lower.startsWith("cni")
        || lower.startsWith("flannel")
        || lower.startsWith("cali")
        || lower.startsWith("virbr")
        || lower.startsWith("podman")
        || lower.startsWith("vmnet")
        || lower.startsWith("vboxnet")
        || lower.startsWith("awdl")
        || lower.startsWith("llw")
        || lower.startsWith("utun")
        || lower.startsWith("dummy")
        || lower.startsWith("zt")
        || lower.startsWith("tailscale")
        || lower.startsWith("wg")
        || lower.startsWith("tun")
        || lower.startsWith("tap")
        || lower.startsWith("isatap")
        || lower.startsWith("teredo")
        || lower.startsWith("bond")
        || lower.startsWith("team");
  }

  private static boolean isLocallyAdministered(byte[] mac) {
    return (mac[0] & 0x02) != 0;
  }

  private static boolean isAllZeros(byte[] bytes) {
    for (byte value : bytes) {
      if (value != 0) {
        return false;
      }
    }
    return true;
  }

  private static boolean isAllOnes(byte[] bytes) {
    for (byte value : bytes) {
      if ((value & 0xFF) != 0xFF) {
        return false;
      }
    }
    return true;
  }

  private static @Nullable String runCommand(String... command) {
    try {
      Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
      CompletableFuture<byte[]> outputFuture =
          CompletableFuture.supplyAsync(
              () -> {
                try {
                  return process.getInputStream().readAllBytes();
                } catch (Exception e) {
                  return null;
                }
              });
      if (!process.waitFor(PROCESS_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
        process.destroyForcibly();
        process.getInputStream().close();
        outputFuture.cancel(true);
        return null;
      }
      if (process.exitValue() != 0) {
        return null;
      }
      byte[] output = outputFuture.get(1, TimeUnit.SECONDS);
      if (output == null) {
        return null;
      }
      return new String(output, StandardCharsets.UTF_8);
    } catch (Exception ignored) {
    }
    return null;
  }

  private static @Nullable String readTextFile(String path) {
    try {
      return normalizeIdentifier(Files.readString(Path.of(path), StandardCharsets.UTF_8));
    } catch (Exception ignored) {
    }
    return null;
  }

  private static @Nullable String readBinaryTextFile(String path) {
    try {
      byte[] bytes = Files.readAllBytes(Path.of(path));
      int length = 0;
      while (length < bytes.length && bytes[length] != 0) {
        length++;
      }
      return normalizeIdentifier(new String(bytes, 0, length, StandardCharsets.UTF_8));
    } catch (Exception ignored) {
    }
    return null;
  }

  private static @Nullable String readRawTextFile(String path) {
    try {
      return Files.readString(Path.of(path), StandardCharsets.UTF_8);
    } catch (Exception ignored) {
    }
    return null;
  }

  private static @Nullable String normalizeIdentifier(@Nullable String rawValue) {
    if (rawValue == null) {
      return null;
    }

    String value = rawValue.trim();
    if (value.isEmpty()) {
      return null;
    }

    String lower = value.toLowerCase(Locale.ROOT);
    if (lower.equals("unknown")
        || lower.equals("none")
        || lower.equals("null")
        || lower.equals("not specified")
        || lower.equals("not available")
        || lower.equals("not settable")
        || lower.equals("not applicable")
        || lower.equals("invalid")
        || lower.equals("n/a")
        || lower.equals("na")
        || lower.equals("default string")
        || lower.equals("system serial number")
        || lower.equals("system product name")
        || lower.equals("system manufacturer")
        || lower.equals("chassis serial number")
        || lower.equals("base board serial number")
        || lower.equals("no asset information")
        || lower.equals("type1productconfigid")
        || lower.equals("o.e.m.")
        || lower.equals("empty")
        || lower.equals("unspecified")
        || lower.equals("default")
        || lower.equals("serial")
        || lower.equals("none specified")
        || lower.startsWith("to be filled")
        || lower.startsWith("0123456789")
        || lower.equals("123456789")
        || lower.equals("1234567890")
        || lower.replace("-", "").equals("03000200040005000006000700080009")) {
      return null;
    }

    String compact = lower.replace("-", "").replace(":", "").replace(" ", "");
    if (compact.length() <= 3) {
      return null;
    }
    if (allCharactersMatch(compact, '0')) {
      return null;
    }
    if (allCharactersMatch(compact, 'f')) {
      return null;
    }
    if (allCharactersMatch(compact, '1')) {
      return null;
    }
    if (allCharactersMatch(compact, 'x')) {
      return null;
    }

    return lower;
  }

  private static boolean allCharactersMatch(String value, char expected) {
    for (int index = 0; index < value.length(); index++) {
      if (value.charAt(index) != expected) {
        return false;
      }
    }
    return true;
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
