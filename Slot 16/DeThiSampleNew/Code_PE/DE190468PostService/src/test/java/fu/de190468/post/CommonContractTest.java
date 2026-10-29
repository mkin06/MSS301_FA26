package fu.de190468.post;

import static org.junit.jupiter.api.Assertions.*;

import fu.de190468.post.common.*;
import org.junit.jupiter.api.Test;

class CommonContractTest {
  @Test
  void paginationRejectsInvalidBoundaries() {
    for (int[] input : new int[][] {{-1, 10}, {0, 0}, {0, 101}}) {
      var ex = assertThrows(ApiException.class, () -> QueryOptions.page(input[0], input[1], "id"));
      assertEquals(406, ex.getHttp());
      assertEquals(2, ex.getStatus());
    }
    assertEquals(100, QueryOptions.page(0, 100, "id").getPageSize());
  }

  @Test
  void nameSearchTreatsWildcardsAsLiterals() {
    assertEquals("%a!%!_![!!%", QueryOptions.contains("a%_[!"));
    assertNull(QueryOptions.contains("  "));
  }

  @Test
  void apiTimestampIsUtcWithSecondPrecision() {
    var response = fu.de190468.post.dto.ApiResponse.ok("Successful", "data");
    assertTrue(response.timestamp().matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z"));
  }
}
