# Sentinel Java Client

Java client library for the [Sentinel](https://demeng.dev/sentinel) API.

## Installation

### Gradle

```kotlin
repositories {
    mavenCentral()
}

dependencies {
    implementation("dev.demeng.sentinel:sentinel-java-client:2.0.0")
}
```

### Maven

```xml
<dependency>
    <groupId>dev.demeng.sentinel</groupId>
    <artifactId>sentinel-java-client</artifactId>
    <version>2.0.0</version>
</dependency>
```

## Quick Start

```java
import dev.demeng.sentinel.client.SentinelClient;
import dev.demeng.sentinel.client.license.validation.*;

SentinelClient client = SentinelClient.builder()
    .baseUrl("https://your-sentinel-instance.com")
    .apiKey("your-api-key")
    .build();

ValidationResult result = client.licenses().validate(
    ValidationRequest.builder()
        .product("my-product")
        .key("LICENSE-KEY")
        .build());

if (result.isValid()) {
    ValidationDetails details = result.getDetails();
    System.out.println("License tier: " + details.tier());
    System.out.println("Expires: " + details.expiration());
} else {
    System.out.println("Validation failed: " + result.getMessage());
}
```

## Validation

Every validation request requires `product` and a license identifier:

- **By key**: provide `key` to look up a specific license directly.
- **By connection**: provide `connectionPlatform` and `connectionValue` (e.g., `"discord"` and a
  user ID) to resolve the license through a platform connection. If the product has auto-creation
  enabled and no matching license exists, one is created automatically.

| Optional Field | Default |
|---|---|
| `server` | `MachineFingerprint.generate()` (see [Utilities](#utilities)) |
| `ip` | Auto-detected from request |

### Validating by Connection

```java
ValidationResult result = client.licenses().validate(
    ValidationRequest.builder()
        .product("my-product")
        .connectionPlatform("discord")
        .connectionValue("123456789")
        .build());
```

### Handling Results

Validation failures return a `ValidationResult` with `isValid() == false` rather than throwing.
Use `requireValid()` if you prefer exceptions:

```java
try {
    ValidationDetails details = client.licenses().validate(request).requireValid();
} catch (LicenseValidationException e) {
    System.out.println("License issue: " + e.getType() + " - " + e.getMessage());
}
```

On success, `ValidationDetails` provides:

| Field | Description |
|---|---|
| `tier()` | License tier name |
| `expiration()` | Expiration time, or null if the license does not expire |
| `serverCount()` / `maxServers()` | Current and maximum server count (-1 for unlimited) |
| `ipCount()` / `maxIps()` | Current and maximum IP count (-1 for unlimited) |
| `entitlements()` | Set of granted entitlements |

### Failure Types

`ValidationResult.getType()` returns one of the following `ValidationResultType` values:

| Type | Meaning |
|---|---|
| `SUCCESS` | License is valid |
| `INVALID_PRODUCT` | Product not found, license belongs to a different product, or auto-creation is not enabled |
| `INVALID_LICENSE` | License key not recognized |
| `INVALID_PLATFORM` | Auto-creation requested but connection info is missing or the platform is not registered |
| `EXPIRED_LICENSE` | License has expired |
| `BLACKLISTED_LICENSE` | License has been blacklisted |
| `CONNECTION_MISMATCH` | Provided connection value does not match the value recorded on the license |
| `EXCESSIVE_SERVERS` | Server limit exceeded |
| `EXCESSIVE_IPS` | IP limit exceeded |

Certain failure types include additional context via `getFailureDetails()`:

```java
ValidationResult result = client.licenses().validate(request);
if (!result.isValid() && result.getFailureDetails() instanceof FailureDetails.BlacklistDetails bd) {
    System.out.println("Blacklisted at " + bd.timestamp() + ": " + bd.reason());
}
```

| Detail type | Applies to | Fields |
|---|---|---|
| `BlacklistDetails` | `BLACKLISTED_LICENSE` | `timestamp()`, `reason()` |
| `ExcessiveServersDetails` | `EXCESSIVE_SERVERS` | `maxServers()` |
| `ExcessiveIpsDetails` | `EXCESSIVE_IPS` | `maxIps()` |

### Signature Verification

Validation responses can be verified with Ed25519 signature checking and replay protection.
This applies only to the validation endpoint.

```java
SentinelClient client = SentinelClient.builder()
    .baseUrl("https://your-sentinel-instance.com")
    .apiKey("your-api-key")
    .publicKey("your-base64-encoded-ed25519-public-key")
    .replayProtectionWindow(Duration.ofSeconds(30))
    .build();
```

With a public key configured, successful validation responses are verified automatically. A
`SignatureVerificationException` is thrown if the signature does not match, and a
`ReplayDetectedException` is thrown if the response nonce was already seen or the timestamp
falls outside the replay protection window.

## License Management

### CRUD

```java
import dev.demeng.sentinel.client.license.*;

// Create
License license = client.licenses().create(
    CreateLicenseRequest.builder()
        .product("my-product")
        .tier("premium")
        .maxServers(3)
        .expiration(Instant.parse("2027-01-01T00:00:00Z"))
        .build());

// Get
License license = client.licenses().get("LICENSE-KEY");

// List with filtering and pagination
Page<License> page = client.licenses().list(
    ListLicensesRequest.builder()
        .product("my-product")
        .status("active")
        .page(0)
        .size(25)
        .build());

// Update (partial, only sends fields that are set)
License updated = client.licenses().update("LICENSE-KEY",
    UpdateLicenseRequest.builder()
        .maxServers(5)
        .note("Upgraded plan")
        .build());

// Delete
client.licenses().delete("LICENSE-KEY");

// Regenerate key (random)
License regenerated = client.licenses().regenerateKey("OLD-KEY");

// Regenerate key (specific)
License regenerated = client.licenses().regenerateKey("OLD-KEY", "NEW-KEY");
```

### Connections, Servers, IPs, and Sub-Users

All operations return the updated `License`.

```java
import dev.demeng.sentinel.client.license.*;

// Connections (platform-to-user mappings)
License updated = client.licenses().connections().add("LICENSE-KEY", Map.of("discord", "123456789"));
client.licenses().connections().remove("LICENSE-KEY", Set.of("discord"));

// Servers
client.licenses().servers().add("LICENSE-KEY", Set.of("server-1", "server-2"));
client.licenses().servers().remove("LICENSE-KEY", Set.of("server-1"));

// IPs
client.licenses().ips().add("LICENSE-KEY", Set.of("192.168.1.1"));
client.licenses().ips().remove("LICENSE-KEY", Set.of("192.168.1.1"));

// Sub-users
client.licenses().subUsers().add("LICENSE-KEY", List.of(new SubUser("discord", "987654321")));
client.licenses().subUsers().remove("LICENSE-KEY", List.of(new SubUser("discord", "987654321")));
```

## Utilities

### MachineFingerprint

Generates a stable, unique identifier for the current machine.

The fingerprint is derived from platform-specific machine identifiers (`/etc/machine-id` on
Linux, `IOPlatformUUID` on macOS, `MachineGuid` on Windows) with a fallback based on system
properties, hostname, and MAC addresses. The result is a 32-character lowercase hex string.

```java
String fingerprint = MachineFingerprint.generate();
```

### PublicIp

Resolves the public IP address of the current machine by querying an external service. The
preferred approach is to configure your reverse proxy to forward the real client IP (e.g.,
`X-Forwarded-For`). This utility exists only as a fallback for environments where that is not
possible.

```java
ValidationRequest request = ValidationRequest.builder()
    .product("my-product")
    .key("LICENSE-KEY")
    .ip(PublicIp.resolve())
    .build();
```

`resolve()` returns `null` if the lookup fails for any reason (timeout, network error, etc.).

## Error Handling

All exceptions extend the sealed `SentinelException`:

| Exception | Cause |
|---|---|
| `SentinelApiException` | API error response (401, 429, 500, etc.) |
| `SentinelConnectionException` | Network failure (timeout, DNS, etc.) |
| `SignatureVerificationException` | Validation response signature mismatch |
| `ReplayDetectedException` | Validation response nonce already seen or timestamp out of range |
| `LicenseValidationException` | `requireValid()` called on a failed result |

## License

[MIT](LICENSE)
