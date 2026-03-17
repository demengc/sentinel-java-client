package dev.demeng.sentinel.client.internal;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.demeng.sentinel.client.license.License;
import dev.demeng.sentinel.client.license.Page;
import java.util.ArrayList;
import java.util.List;

public final class PageResponseParser {

  private final LicenseResponseParser licenseParser;

  public PageResponseParser(LicenseResponseParser licenseParser) {
    this.licenseParser = licenseParser;
  }

  public Page<License> parse(JsonObject result) {
    JsonObject pageWrapper = result.getAsJsonObject("page");

    JsonArray contentArray = pageWrapper.getAsJsonArray("content");
    List<License> licenses = new ArrayList<>(contentArray.size());
    for (JsonElement el : contentArray) {
      licenses.add(licenseParser.parseLicenseObject(el.getAsJsonObject()));
    }

    JsonObject metadata = pageWrapper.getAsJsonObject("page");
    int size = metadata.get("size").getAsInt();
    int number = metadata.get("number").getAsInt();
    long totalElements = metadata.get("totalElements").getAsLong();
    int totalPages = metadata.get("totalPages").getAsInt();

    return new Page<>(licenses, size, number, totalElements, totalPages);
  }
}
