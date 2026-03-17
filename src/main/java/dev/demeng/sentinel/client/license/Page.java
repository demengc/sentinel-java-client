package dev.demeng.sentinel.client.license;

import java.util.List;

public record Page<T>(List<T> content, int size, int number, long totalElements, int totalPages) {
  public Page {
    content = content == null ? List.of() : List.copyOf(content);
  }
}
