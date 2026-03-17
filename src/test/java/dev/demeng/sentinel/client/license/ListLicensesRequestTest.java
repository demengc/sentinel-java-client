package dev.demeng.sentinel.client.license;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import org.junit.jupiter.api.Test;

class ListLicensesRequestTest {

  @Test
  void defaultPageAndSize() {
    ListLicensesRequest req = ListLicensesRequest.builder().build();
    Map<String, String> params = req.toQueryParams();

    assertEquals("0", params.get("page"));
    assertEquals("50", params.get("size"));
    assertEquals(2, params.size());
  }

  @Test
  void allFiltersIncluded() {
    ListLicensesRequest req =
        ListLicensesRequest.builder()
            .product("MyProduct")
            .status("active")
            .query("KEY")
            .platform("Discord")
            .value("user123")
            .server("srv-1")
            .ip("1.2.3.4")
            .page(2)
            .size(25)
            .build();

    Map<String, String> params = req.toQueryParams();
    assertEquals("MyProduct", params.get("product"));
    assertEquals("active", params.get("status"));
    assertEquals("KEY", params.get("query"));
    assertEquals("Discord", params.get("platform"));
    assertEquals("user123", params.get("value"));
    assertEquals("srv-1", params.get("server"));
    assertEquals("1.2.3.4", params.get("ip"));
    assertEquals("2", params.get("page"));
    assertEquals("25", params.get("size"));
  }

  @Test
  void nullFiltersOmitted() {
    ListLicensesRequest req = ListLicensesRequest.builder().product("Prod").build();

    Map<String, String> params = req.toQueryParams();
    assertTrue(params.containsKey("product"));
    assertFalse(params.containsKey("status"));
    assertFalse(params.containsKey("query"));
  }
}
