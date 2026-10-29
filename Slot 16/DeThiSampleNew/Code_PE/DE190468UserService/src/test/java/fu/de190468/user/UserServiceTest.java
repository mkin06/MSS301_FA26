package fu.de190468.user;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import fu.de190468.user.common.*;
import fu.de190468.user.dto.*;
import fu.de190468.user.entity.*;
import fu.de190468.user.repository.UserRepository;
import fu.de190468.user.service.impl.UserServiceImpl;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
  @Mock UserRepository repository;
  UserServiceImpl service;

  @BeforeEach
  void init() {
    service = new UserServiceImpl(repository);
  }

  UserDTO request() {
    var d = new UserDTO();
    d.setUsername("unique");
    d.setRole("CREATOR");
    d.setRatePerPost(new BigDecimal("500000"));
    d.setMaxDailyPosts(2);
    d.setLevel(1);
    d.setEmail("test@example.com");
    return d;
  }

  User entity() {
    var e = new User();
    e.setUserId(1L);
    e.setUsername("original");
    e.setRole(Role.CREATOR);
    e.setRatePerPost(new BigDecimal("500000"));
    e.setMaxDailyPosts(2);
    e.setLevel(1);
    e.setStatus(UserStatus.ACTIVE);
    e.setEmail("old@example.com");
    return e;
  }

  @Test
  void createSetsDefaultStatusAndReturnsGeneratedId() {
    when(repository.saveAndFlush(any()))
        .thenAnswer(
            call -> {
              User e = call.getArgument(0);
              e.setUserId(1L);
              return e;
            });
    var result = service.create(request());
    assertEquals(1, result.status());
    assertEquals(1L, result.data().getUserId());
    assertEquals("ACTIVE", result.data().getStatus());
  }

  @Test
  void duplicateCreateReturns226WithoutSaving() {
    when(repository.existsByUsername("unique")).thenReturn(true);
    var ex = assertThrows(ApiException.class, () -> service.create(request()));
    assertEquals(226, ex.getHttp());
    assertEquals(3, ex.getStatus());
    verify(repository, never()).saveAndFlush(any());
  }

  @Test
  void databaseUniqueRaceAlsoReturns226() {
    when(repository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("unique"));
    assertEquals(226, assertThrows(ApiException.class, () -> service.create(request())).getHttp());
  }

  @Test
  void partialUpdateRetainsOmittedFields() {
    var e = entity();
    when(repository.findById(1L)).thenReturn(Optional.of(e));
    when(repository.saveAndFlush(any())).thenAnswer(call -> call.getArgument(0));
    var d = new UserDTO();
    d.setLevel(3);
    var result = service.update(1L, d).data();
    assertEquals(3, result.getLevel());
    assertEquals("original", result.getUsername());
    assertEquals(new BigDecimal("500000"), result.getRatePerPost());
  }

  @Test
  void updateExcludesItsOwnIdFromDuplicateCheck() {
    when(repository.findById(1L)).thenReturn(Optional.of(entity()));
    when(repository.saveAndFlush(any())).thenAnswer(call -> call.getArgument(0));
    var d = new UserDTO();
    d.setUsername("original");
    service.update(1L, d);
    verify(repository).existsByUsernameAndUserIdNot("original", 1L);
  }

  @Test
  void duplicateUpdateDoesNotMutateEntity() {
    var e = entity();
    when(repository.findById(1L)).thenReturn(Optional.of(e));
    when(repository.existsByUsernameAndUserIdNot("unique", 1L)).thenReturn(true);
    assertEquals(
        226, assertThrows(ApiException.class, () -> service.update(1L, request())).getHttp());
    assertEquals("original", e.getUsername());
  }

  @Test
  void allMissingOperationsReturn404() {
    assertEquals(404, assertThrows(ApiException.class, () -> service.get(99L)).getHttp());
    assertEquals(
        404, assertThrows(ApiException.class, () -> service.update(99L, new UserDTO())).getHttp());
    assertEquals(404, assertThrows(ApiException.class, () -> service.delete(99L)).getHttp());
  }

  @Test
  void deleteIsSoftAndDoesNotRemoveRow() {
    var e = entity();
    when(repository.findById(1L)).thenReturn(Optional.of(e));
    var result = service.delete(1L);
    assertEquals(UserStatus.DEACTIVATED, e.getStatus());
    assertNull(result.data());
    verify(repository).save(e);
    verify(repository, never()).delete(any());
  }

  @Test
  void invalidEnumFilterReturns406() {
    assertEquals(
        406,
        assertThrows(ApiException.class, () -> service.list(0, 10, "INVALID", null)).getHttp());
  }
}
