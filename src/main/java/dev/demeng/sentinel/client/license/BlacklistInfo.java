package dev.demeng.sentinel.client.license;

import java.time.Instant;
import org.jspecify.annotations.Nullable;

public record BlacklistInfo(Instant timestamp, @Nullable String reason) {}
