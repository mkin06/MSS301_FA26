package com.mss301.account;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.mss301.account.dto.*;
import com.mss301.account.entity.StoreAccount;
import com.mss301.account.repository.AccountRepository;
import com.mss301.account.service.*;
import java.util.Optional;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
  @Mock AccountRepository repository;
  @Mock PasswordEncoder encoder;
  JwtService jwt = new JwtService(JwtServiceTest.SECRET, 900000, 604800000);
  AuthService service;

  @BeforeEach
  void init() {
    service = new AuthService(repository, encoder, jwt);
  }

  StoreAccount account(boolean active) {
    return StoreAccount.builder()
        .accountId(1)
        .username("adminsys")
        .password("hash")
        .role(1)
        .isActive(active)
        .build();
  }

  LoginRequest request() {
    var r = new LoginRequest();
    r.setUsername("adminsys");
    r.setPassword("@1");
    return r;
  }

  @Test
  void builderDefaultsToActive() {
    assertTrue(StoreAccount.builder().build().getIsActive());
  }

  @Test
  void inactiveIsCheckedBeforePassword() {
    when(repository.findByUsername("adminsys")).thenReturn(Optional.of(account(false)));
    assertEquals(
        403,
        assertThrows(ResponseStatusException.class, () -> service.login(request()))
            .getStatusCode()
            .value());
    verifyNoInteractions(encoder);
  }

  @Test
  void unknownAccountIsUnauthorized() {
    assertEquals(
        401,
        assertThrows(ResponseStatusException.class, () -> service.login(request()))
            .getStatusCode()
            .value());
    verifyNoInteractions(encoder);
  }

  @Test
  void wrongPasswordIsUnauthorized() {
    when(repository.findByUsername("adminsys")).thenReturn(Optional.of(account(true)));
    assertEquals(
        401,
        assertThrows(ResponseStatusException.class, () -> service.login(request()))
            .getStatusCode()
            .value());
  }

  @Test
  void loginReturnsTokensWithoutPassword() {
    when(repository.findByUsername("adminsys")).thenReturn(Optional.of(account(true)));
    when(encoder.matches("@1", "hash")).thenReturn(true);
    var r = service.login(request());
    assertEquals("Bearer", r.getTokenType());
    assertEquals(1, r.getRole());
    assertEquals("access", jwt.parseToken(r.getAccessToken()).get("type"));
  }

  @Test
  void refreshRejectsAccessToken() {
    assertEquals(
        401,
        assertThrows(
                ResponseStatusException.class,
                () -> service.refresh(jwt.generateAccessToken("1", 1)))
            .getStatusCode()
            .value());
    verifyNoInteractions(repository);
  }

  @Test
  void refreshRejectsMalformedAndAbsentToken() {
    for (String token : new String[] {"bad", null, ""})
      assertEquals(
          401,
          assertThrows(ResponseStatusException.class, () -> service.refresh(token))
              .getStatusCode()
              .value());
  }

  @Test
  void refreshRejectsInactiveAccount() {
    when(repository.findById(1)).thenReturn(Optional.of(account(false)));
    assertEquals(
        403,
        assertThrows(
                ResponseStatusException.class, () -> service.refresh(jwt.generateRefreshToken("1")))
            .getStatusCode()
            .value());
  }

  @Test
  void refreshRejectsRemovedAccount() {
    assertEquals(
        401,
        assertThrows(
                ResponseStatusException.class, () -> service.refresh(jwt.generateRefreshToken("1")))
            .getStatusCode()
            .value());
  }

  @Test
  void refreshRejectsInvalidSubject() {
    assertEquals(
        401,
        assertThrows(
                ResponseStatusException.class,
                () -> service.refresh(jwt.generateRefreshToken("bad")))
            .getStatusCode()
            .value());
  }

  @Test
  void refreshReturnsNewPair() {
    when(repository.findById(1)).thenReturn(Optional.of(account(true)));
    var old = jwt.generateRefreshToken("1");
    var r = service.refresh(old);
    assertNotEquals(old, r.getRefreshToken());
    assertEquals("refresh", jwt.parseToken(r.getRefreshToken()).get("type"));
    verifyNoInteractions(encoder);
  }
}
