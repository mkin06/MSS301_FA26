package fu.de190468.post.common;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public final class QueryOptions {
  private QueryOptions() {}

  public static Pageable page(int page, int size, String id) {
    if (page < 0 || size < 1 || size > 100)
      throw ApiException.invalid("page must be >= 0; size must be between 1 and 100");
    return PageRequest.of(page, size, Sort.by(id));
  }

  public static <E extends Enum<E>> E filter(String value, Class<E> type) {
    if (value == null || value.isBlank()) return null;
    try {
      return Enum.valueOf(type, value);
    } catch (IllegalArgumentException ex) {
      throw ApiException.invalid("Invalid " + type.getSimpleName());
    }
  }

  public static String contains(String value) {
    if (value == null || value.isBlank()) return null;
    return "%"
        + value.replace("!", "!!").replace("%", "!%").replace("_", "!_").replace("[", "![")
        + "%";
  }
}
