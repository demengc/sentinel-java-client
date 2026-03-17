package dev.demeng.sentinel.client.license;

import org.jspecify.annotations.Nullable;

public record LicenseProduct(
    String id, String name, @Nullable String description, @Nullable String logoUrl) {}
