package com.mss301.account.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

// [TODO-3.2] HS256 keys come from the environment; no shared default secret.
@Service
public class JwtService {
  private final SecretKey key;
  private final long expiration;
  private final long refreshExpiration;

  public JwtService(
      @Value("${jwt.secret}") String secret,
      @Value("${jwt.expiration}") long expiration,
      @Value("${jwt.refresh-expiration}") long refreshExpiration) {
    if (secret.getBytes(StandardCharsets.UTF_8).length < 32)
      throw new IllegalArgumentException("JWT_SECRET must have at least 32 bytes");
    if (expiration <= 0 || refreshExpiration <= 0)
      throw new IllegalArgumentException("Token durations must be positive");
    this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    this.expiration = expiration;
    this.refreshExpiration = refreshExpiration;
  }

  public String generateAccessToken(String subject, Integer role) {
    return Jwts.builder()
        .id(UUID.randomUUID().toString())
        .subject(subject)
        .claim("role", role)
        .claim("type", "access")
        .issuedAt(new Date())
        .expiration(new Date(System.currentTimeMillis() + expiration))
        .signWith(key, Jwts.SIG.HS256)
        .compact();
  }

  public String generateRefreshToken(String subject) {
    return Jwts.builder()
        .id(UUID.randomUUID().toString())
        .subject(subject)
        .claim("type", "refresh")
        .issuedAt(new Date())
        .expiration(new Date(System.currentTimeMillis() + refreshExpiration))
        .signWith(key, Jwts.SIG.HS256)
        .compact();
  }

  public Claims parseToken(String token) {
    return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
  }
}
