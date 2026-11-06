package com.mss301.account.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
// [TODO-3.4]
@Data public class LoginRequest {
    @NotBlank @Size(max=20) private String username;
    @NotBlank @Size(max=100) private String password;
}
