package com.mss301.account;

import static org.junit.jupiter.api.Assertions.*;

import com.mss301.account.service.JwtService;
import io.jsonwebtoken.*;
import org.junit.jupiter.api.Test;

class JwtServiceTest {
  static final String SECRET = "test-key-at-least-thirty-two-bytes-long-for-unit-tests";
  JwtService jwt = new JwtService(SECRET, 900000, 604800000);

  @Test
  void accessHasRoleTypeAndFifteenMinuteLifetime() {
    var c = jwt.parseToken(jwt.generateAccessToken("1", 1));
    assertEquals("1", c.getSubject());
    assertEquals(1, c.get("role", Integer.class));
    assertEquals("access", c.get("type"));
    assertEquals(900000, c.getExpiration().getTime() - c.getIssuedAt().getTime());
  }

  @Test
  void refreshHasSevenDayLifetimeAndNoRole() {
    var c = jwt.parseToken(jwt.generateRefreshToken("1"));
    assertEquals("refresh", c.get("type"));
    assertFalse(c.containsKey("role"));
    assertEquals(604800000, c.getExpiration().getTime() - c.getIssuedAt().getTime());
  }

  @Test
  void shortSecretIsRejected() {
    assertThrows(IllegalArgumentException.class, () -> new JwtService("short", 1, 1));
  }

  @Test
  void invalidDurationIsRejected() {
    assertThrows(IllegalArgumentException.class, () -> new JwtService(SECRET, 0, 1));
  }

  @Test
  void wrongSignatureIsRejected() {
    var other = new JwtService(SECRET + "different", 900000, 604800000);
    assertThrows(JwtException.class, () -> jwt.parseToken(other.generateRefreshToken("1")));
  }

  @Test
  void malformedTokenIsRejected() {
    assertThrows(JwtException.class, () -> jwt.parseToken("bad-token"));
  }

  @Test
  void expiredTokenIsRejected() {
    var key =
        io.jsonwebtoken.security.Keys.hmacShaKeyFor(
            SECRET.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    var token =
        Jwts.builder().subject("1").expiration(new java.util.Date(1)).signWith(key).compact();
    assertThrows(ExpiredJwtException.class, () -> jwt.parseToken(token));
  }

  @Test
  void refreshedTokensHaveDistinctIds() {
    assertNotEquals(jwt.generateRefreshToken("1"), jwt.generateRefreshToken("1"));
  }
}
