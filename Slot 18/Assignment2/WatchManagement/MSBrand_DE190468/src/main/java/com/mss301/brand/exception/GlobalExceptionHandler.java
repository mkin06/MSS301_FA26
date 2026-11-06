package com.mss301.brand.exception;

import com.mss301.brand.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

// [TODO-5.4]
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
  @ExceptionHandler(ResponseStatusException.class)
  public ResponseEntity<ApiResponse<Void>> status(ResponseStatusException ex) {
    return ResponseEntity.status(ex.getStatusCode()).body(ApiResponse.error(ex.getReason()));
  }

  @ExceptionHandler({
    MethodArgumentTypeMismatchException.class,
    HandlerMethodValidationException.class
  })
  public ResponseEntity<ApiResponse<Void>> invalid(Exception ex) {
    return ResponseEntity.badRequest().body(ApiResponse.error("Invalid identifier"));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiResponse<Void>> unexpected(Exception ex) {
    log.error("Brand request failed", ex);
    return ResponseEntity.internalServerError().body(ApiResponse.error("Internal server error"));
  }
}
