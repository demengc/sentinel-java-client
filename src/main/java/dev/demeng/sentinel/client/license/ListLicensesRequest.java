package dev.demeng.sentinel.client.license;

import java.util.LinkedHashMap;
import java.util.Map;
import org.jspecify.annotations.Nullable;

public final class ListLicensesRequest {

  private final @Nullable String product;
  private final @Nullable String status;
  private final @Nullable String query;
  private final @Nullable String platform;
  private final @Nullable String value;
  private final @Nullable String server;
  private final @Nullable String ip;
  private final int page;
  private final int size;

  private ListLicensesRequest(Builder builder) {
    this.product = builder.product;
    this.status = builder.status;
    this.query = builder.query;
    this.platform = builder.platform;
    this.value = builder.value;
    this.server = builder.server;
    this.ip = builder.ip;
    this.page = builder.page;
    this.size = builder.size;
  }

  public static Builder builder() {
    return new Builder();
  }

  public Map<String, String> toQueryParams() {
    Map<String, String> params = new LinkedHashMap<>();
    if (product != null) params.put("product", product);
    if (status != null) params.put("status", status);
    if (query != null) params.put("query", query);
    if (platform != null) params.put("platform", platform);
    if (value != null) params.put("value", value);
    if (server != null) params.put("server", server);
    if (ip != null) params.put("ip", ip);
    params.put("page", String.valueOf(page));
    params.put("size", String.valueOf(size));
    return params;
  }

  public static final class Builder {

    private @Nullable String product;
    private @Nullable String status;
    private @Nullable String query;
    private @Nullable String platform;
    private @Nullable String value;
    private @Nullable String server;
    private @Nullable String ip;
    private int page = 0;
    private int size = 50;

    private Builder() {}

    public Builder product(String product) {
      this.product = product;
      return this;
    }

    public Builder status(String status) {
      this.status = status;
      return this;
    }

    public Builder query(String query) {
      this.query = query;
      return this;
    }

    public Builder platform(String platform) {
      this.platform = platform;
      return this;
    }

    public Builder value(String value) {
      this.value = value;
      return this;
    }

    public Builder server(String server) {
      this.server = server;
      return this;
    }

    public Builder ip(String ip) {
      this.ip = ip;
      return this;
    }

    public Builder page(int page) {
      this.page = page;
      return this;
    }

    public Builder size(int size) {
      this.size = size;
      return this;
    }

    public ListLicensesRequest build() {
      return new ListLicensesRequest(this);
    }
  }
}
