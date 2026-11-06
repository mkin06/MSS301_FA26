package com.mss301.account.dto;

// [TODO-3.4] Shared JSON envelope for success and failure.
public record ApiResponse<T>(boolean success, String message, T data) {
  public static <T> ApiResponse<T> success(T data, String message) {
    return new ApiResponse<>(true, message, data);
  }

  public static ApiResponse<Void> error(String message) {
    return new ApiResponse<>(false, message, null);
  }
}
