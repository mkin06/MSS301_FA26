package fu.de190468.post.common;

import fu.de190468.post.dto.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {
  private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(ApiException.class)
  public ResponseEntity<ApiResponse<Object>> business(ApiException ex) {
    return ResponseEntity.status(ex.getHttp())
        .body(ApiResponse.of(ex.getStatus(), ex.getMessage(), null));
  }

  @ExceptionHandler({
    MethodArgumentNotValidException.class,
    ConstraintViolationException.class,
    HttpMessageNotReadableException.class,
    HandlerMethodValidationException.class,
    MethodArgumentTypeMismatchException.class,
    MissingServletRequestParameterException.class
  })
  public ResponseEntity<ApiResponse<Object>> invalid(Exception ex) {
    String message = "Validation failed";
    if (ex instanceof MethodArgumentNotValidException validation) {
      message =
          validation.getBindingResult().getFieldErrors().stream()
              .map(e -> e.getField() + ": " + e.getDefaultMessage())
              .sorted()
              .collect(java.util.stream.Collectors.joining("; "));
    }
    return ResponseEntity.status(406).body(ApiResponse.of(2, message, null));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiResponse<Object>> unexpected(Exception ex) {
    LOG.error("Request failed", ex);
    return ResponseEntity.status(500).body(ApiResponse.of(0, "Internal server error", null));
  }
}
