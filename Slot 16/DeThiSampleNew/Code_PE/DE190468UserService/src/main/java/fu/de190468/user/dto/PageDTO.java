package fu.de190468.user.dto;

import java.util.List;
import org.springframework.data.domain.Page;

public record PageDTO<T>(
    int size,
    int page,
    int totalPages,
    long totalElements,
    boolean first,
    boolean last,
    List<T> content) {
  public static <S, T> PageDTO<T> from(Page<S> page, List<T> content) {
    return new PageDTO<>(
        page.getSize(),
        page.getNumber(),
        page.getTotalPages(),
        page.getTotalElements(),
        page.isFirst(),
        page.isLast(),
        content);
  }
}
