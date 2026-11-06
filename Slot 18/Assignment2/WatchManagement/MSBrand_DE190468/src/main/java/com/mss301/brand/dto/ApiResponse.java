package com.mss301.brand.dto;
// [TODO-5.4] Errors and successes share an explicit envelope.
public record ApiResponse<T>(boolean success,String message,T data) {
    public static <T> ApiResponse<T> success(T data,String message) {return new ApiResponse<>(true,message,data);}
    public static ApiResponse<Void> error(String message) {return new ApiResponse<>(false,message,null);}
}
