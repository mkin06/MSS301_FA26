package com.mss301.account.exception;

import com.mss301.account.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

// [TODO-3.4] Keep validation/business errors distinct from server faults.
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
  @ExceptionHandler(ResponseStatusException.class)
  public ResponseEntity<ApiResponse<Void>> status(ResponseStatusException ex) {
    return ResponseEntity.status(ex.getStatusCode()).body(ApiResponse.error(ex.getReason()));
  }

  @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class})
  public ResponseEntity<ApiResponse<Void>> invalid(Exception ex) {
    return ResponseEntity.badRequest().body(ApiResponse.error("Invalid request"));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiResponse<Void>> unexpected(Exception ex) {
    log.error("Account request failed", ex);
    return ResponseEntity.internalServerError().body(ApiResponse.error("Internal server error"));
  }
}
