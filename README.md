# Sentinel Java Client

[![Maven Central](https://img.shields.io/maven-central/v/dev.demeng.sentinel/sentinel-java-client)](https://central.sonatype.com/artifact/dev.demeng.sentinel/sentinel-java-client)

Java client library for the [Sentinel](https://demeng.dev/sentinel) API.

## Installation

This library is published to Maven Central.

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
import dev.demeng.sentinel.client.validation.*;

SentinelClient client = SentinelClient.builder()
    .baseUrl("https://your-sentinel-instance.com")
    .apiKey("your-api-key")
    .build();

ValidationResult result = client.validate(
    ValidationRequest.builder()
        .product("my-product")
        .server("server-1")
        .build());

if (result.isValid()) {
    LicenseDetails details = result.getDetails();
    System.out.println("License tier: " + details.tier());
    System.out.println("Expires: " + details.expiration());
} else {
    System.out.println("Validation failed: " + result.getMessage());
}
```

## Signature Verification

Enable Ed25519 signature verification and replay protection:

```java
SentinelClient client = SentinelClient.builder()
    .baseUrl("https://your-sentinel-instance.com")
    .apiKey("your-api-key")
    .publicKey("your-base64-encoded-ed25519-public-key")
    .replayProtectionWindow(Duration.ofSeconds(30))
    .build();
```

With a public key configured, all successful responses are verified automatically.

## Error Handling

All exceptions extend the sealed `SentinelException`:

| Exception | Cause |
|---|---|
| `SentinelApiException` | API error response (401, 429, 500, etc.) |
| `SentinelConnectionException` | Network failure (timeout, DNS, etc.) |
| `SignatureVerificationException` | Response signature mismatch |
| `ReplayDetectedException` | Response nonce already seen |
| `LicenseValidationException` | `requireValid()` called on a failed result |

Validation failures (expired, blacklisted, etc.) return a `ValidationResult` with
`isValid() == false` rather than throwing. Use `requireValid()` if you prefer exceptions:

```java
try {
    LicenseDetails details = client.validate(request).requireValid();
} catch (LicenseValidationException e) {
    System.out.println("License issue: " + e.getType() + " - " + e.getMessage());
}
```

### Failure Details

Certain failure types include additional context via `getFailureDetails()`:

```java
ValidationResult result = client.validate(request);
if (!result.isValid() && result.getFailureDetails() instanceof FailureDetails.BlacklistDetails bd) {
    System.out.println("Blacklisted at " + bd.timestamp() + ": " + bd.reason());
}
```

Supported detail types: `BlacklistDetails`, `ExcessiveServersDetails`, `ExcessiveIpsDetails`.

## License

[MIT](LICENSE)
