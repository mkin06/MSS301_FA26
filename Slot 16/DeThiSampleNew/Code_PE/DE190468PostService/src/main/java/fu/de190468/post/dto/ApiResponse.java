package fu.de190468.post.dto;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public record ApiResponse<T>(int status, String message, T data, String timestamp) {
  public static <T> ApiResponse<T> ok(String message, T data) {
    return of(1, message, data);
  }

  public static <T> ApiResponse<T> of(int status, String message, T data) {
    return new ApiResponse<>(
        status, message, data, Instant.now().truncatedTo(ChronoUnit.SECONDS).toString());
  }
}
