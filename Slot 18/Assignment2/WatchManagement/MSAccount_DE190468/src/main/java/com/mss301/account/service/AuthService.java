package com.mss301.account.service;

import com.mss301.account.dto.*;
import com.mss301.account.entity.StoreAccount;
import com.mss301.account.repository.AccountRepository;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

// [TODO-3.3] Inactive check precedes password verification, including refresh.
@Service
@RequiredArgsConstructor
public class AuthService {
  private final AccountRepository repository;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwt;

  public LoginResponse login(LoginRequest request) {
    var account =
        repository
            .findByUsername(request.getUsername())
            .orElseThrow(() -> unauthorized("Invalid username or password"));
    active(account);
    if (!passwordEncoder.matches(request.getPassword(), account.getPassword()))
      throw unauthorized("Invalid username or password");
    return tokens(account);
  }

  public LoginResponse refresh(String token) {
    io.jsonwebtoken.Claims claims;
    try {
      claims = jwt.parseToken(token);
    } catch (JwtException | IllegalArgumentException ex) {
      throw unauthorized("Invalid or expired refresh token");
    }
    if (!"refresh".equals(claims.get("type"))) throw unauthorized("Token is not a refresh token");
    int id;
    try {
      id = Integer.parseInt(claims.getSubject());
    } catch (NumberFormatException ex) {
      throw unauthorized("Invalid refresh token subject");
    }
    var account = repository.findById(id).orElseThrow(() -> unauthorized("Account not found"));
    active(account);
    return tokens(account);
  }

  private void active(StoreAccount account) {
    if (!Boolean.TRUE.equals(account.getIsActive()))
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account is inactive");
  }

  private LoginResponse tokens(StoreAccount account) {
    return LoginResponse.builder()
        .accessToken(jwt.generateAccessToken(account.getAccountId().toString(), account.getRole()))
        .refreshToken(jwt.generateRefreshToken(account.getAccountId().toString()))
        .tokenType("Bearer")
        .role(account.getRole())
        .username(account.getUsername())
        .build();
  }

  private ResponseStatusException unauthorized(String message) {
    return new ResponseStatusException(HttpStatus.UNAUTHORIZED, message);
  }
}
