package fu.de190468.post;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import feign.FeignException;
import feign.Request;
import feign.Response;
import fu.de190468.post.client.UserClient;
import fu.de190468.post.common.*;
import fu.de190468.post.dto.*;
import fu.de190468.post.entity.*;
import fu.de190468.post.repository.PostRepository;
import fu.de190468.post.service.impl.PostServiceImpl;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {
  @Mock PostRepository repository;
  @Mock UserClient client;
  PostServiceImpl service;

  @BeforeEach
  void init() {
    service = new PostServiceImpl(repository, client);
  }

  Date day(String value) {
    return java.sql.Date.valueOf(value);
  }

  UserDTO parent() {
    var p = new UserDTO();
    p.setUserId(1L);
    p.setStatus("ACTIVE");
    p.setRatePerPost(new BigDecimal("500000"));
    p.setMaxDailyPosts(3);
    return p;
  }

  void remote(UserDTO p) {
    when(client.get(1L)).thenReturn(ApiResponse.ok("Successful", p));
  }

  PostDTO request() {
    var d = new PostDTO();
    d.setAdvertiserName("Shop");
    d.setAdvertiserEmail("ads@example.com");
    d.setAdvertiserPhone("0900000000");
    d.setStartDate(day("2026-07-25"));
    d.setEndDate(day("2026-07-28"));
    d.setNumberOfPosts(2);
    d.setUserId(1L);
    return d;
  }

  Post entity() {
    var e = new Post();
    e.setPostId(2L);
    e.setUserId(1L);
    e.setStartDate(day("2026-07-25"));
    e.setEndDate(day("2026-07-28"));
    e.setNumberOfPosts(2);
    e.setStatus(PostStatus.SCHEDULED);
    e.setTotalCost(new BigDecimal("1500000"));
    return e;
  }

  @Test
  void createComputesDaysAndIgnoresClientAmountAndStatus() {
    remote(parent());
    when(repository.save(any())).thenAnswer(call -> call.getArgument(0));
    var d = request();
    d.setTotalCost(BigDecimal.ONE);
    d.setStatus("CANCELLED");
    var result = service.create(d).data();
    assertEquals(new BigDecimal("1500000"), result.getTotalCost());
    assertEquals("SCHEDULED", result.getStatus());
    verify(client).get(1L);
  }

  @Test
  void invalidDatesAreRejectedBeforeCallingRemoteService() {
    var d = request();
    d.setEndDate(d.getStartDate());
    assertEquals(406, assertThrows(ApiException.class, () -> service.create(d)).getHttp());
    verifyNoInteractions(client, repository);
  }

  @Test
  void nonexistentParentFeign404Becomes404() {
    Request req =
        Request.create(
            Request.HttpMethod.GET,
            "http://localhost/api/parent/1",
            Map.of(),
            null,
            StandardCharsets.UTF_8,
            null);
    var ex =
        FeignException.errorStatus(
            "get",
            Response.builder()
                .status(404)
                .reason("Not Found")
                .request(req)
                .headers(Map.of())
                .build());
    when(client.get(1L)).thenThrow(ex);
    var result = assertThrows(ApiException.class, () -> service.create(request()));
    assertEquals(404, result.getHttp());
    assertEquals(4, result.getStatus());
    verifyNoInteractions(repository);
  }

  @Test
  void inactiveParentReturns400AndNeverSaves() {
    var p = parent();
    p.setStatus("SUSPENDED");
    remote(p);
    var ex = assertThrows(ApiException.class, () -> service.create(request()));
    assertEquals(400, ex.getHttp());
    assertEquals(5, ex.getStatus());
    verifyNoInteractions(repository);
  }

  @Test
  void limitIsCheckedAgainstRemoteParent() {
    remote(parent());
    var d = request();
    d.setNumberOfPosts(4);
    assertEquals(406, assertThrows(ApiException.class, () -> service.create(d)).getHttp());
    verifyNoInteractions(repository);
  }

  @Test
  void invalidPartialDatesNeverDirtyManagedEntity() {
    var e = entity();
    when(repository.findById(2L)).thenReturn(Optional.of(e));
    var d = new PostDTO();
    d.setEndDate(day("2026-07-24"));
    assertEquals(406, assertThrows(ApiException.class, () -> service.update(2L, d)).getHttp());
    assertEquals(day("2026-07-28"), e.getEndDate());
    verifyNoInteractions(client);
    verify(repository, never()).save(any());
  }

  @Test
  void partialUpdateUsesCurrentRateAndPreservesOmittedDates() {
    var e = entity();
    when(repository.findById(2L)).thenReturn(Optional.of(e));
    var p = parent();
    p.setRatePerPost(new BigDecimal("600000"));
    remote(p);
    when(repository.save(any())).thenAnswer(call -> call.getArgument(0));
    var d = new PostDTO();
    d.setAdvertiserName("Updated");
    var result = service.update(2L, d).data();
    assertEquals(new BigDecimal("1800000"), result.getTotalCost());
    assertEquals(day("2026-07-25"), result.getStartDate());
    assertEquals(2, result.getNumberOfPosts());
  }

  @Test
  void partialUpdateRevalidatesRemoteCapacity() {
    var e = entity();
    when(repository.findById(2L)).thenReturn(Optional.of(e));
    remote(parent());
    var d = new PostDTO();
    d.setNumberOfPosts(4);
    assertEquals(406, assertThrows(ApiException.class, () -> service.update(2L, d)).getHttp());
    assertEquals(2, e.getNumberOfPosts());
    verify(repository, never()).save(any());
  }

  @Test
  void partialUpdateCanChangeParentId() {
    when(repository.findById(2L)).thenReturn(Optional.of(entity()));
    when(client.get(3L)).thenReturn(ApiResponse.ok("Successful", parent()));
    when(repository.save(any())).thenAnswer(call -> call.getArgument(0));
    var d = new PostDTO();
    d.setUserId(3L);
    assertEquals(3L, service.update(2L, d).data().getUserId());
    verify(client).get(3L);
  }

  @Test
  void detailEmbedsFullRemoteObject() {
    var e = entity();
    when(repository.findById(2L)).thenReturn(Optional.of(e));
    var p = parent();
    remote(p);
    assertSame(p, service.get(2L).data().getUser());
  }

  @Test
  void deleteCancelsWithoutDeletingAndWithoutRemoteCall() {
    var e = entity();
    when(repository.findById(2L)).thenReturn(Optional.of(e));
    assertNull(service.delete(2L).data());
    assertEquals(PostStatus.CANCELLED, e.getStatus());
    verifyNoInteractions(client);
    verify(repository, never()).delete(any());
  }

  @Test
  void missingBookingOperationsReturn404() {
    assertEquals(404, assertThrows(ApiException.class, () -> service.get(99L)).getHttp());
    assertEquals(
        404, assertThrows(ApiException.class, () -> service.update(99L, request())).getHttp());
    assertEquals(404, assertThrows(ApiException.class, () -> service.delete(99L)).getHttp());
  }

  @Test
  void remoteOutageIsNotMisreportedAsMissingParent() {
    when(client.get(1L)).thenThrow(new IllegalStateException("offline"));
    assertThrows(IllegalStateException.class, () -> service.create(request()));
    verifyNoInteractions(repository);
  }

  @Test
  void amountOverflowIsValidationFailure() {
    var p = parent();
    p.setRatePerPost(new BigDecimal("9999999999999999.99"));
    remote(p);
    assertEquals(406, assertThrows(ApiException.class, () -> service.create(request())).getHttp());
    verifyNoInteractions(repository);
  }

  @Test
  void remoteErrorEnvelopeIsNotMisreportedAs404() {
    when(client.get(1L)).thenReturn(ApiResponse.of(0, "Server error", null));
    var ex = assertThrows(ApiException.class, () -> service.create(request()));
    assertEquals(500, ex.getHttp());
    assertEquals(0, ex.getStatus());
    verifyNoInteractions(repository);
  }

  @Test
  void remoteEmptySuccessfulEnvelopeIsServerFailure() {
    when(client.get(1L)).thenReturn(ApiResponse.ok("Successful", null));
    assertEquals(500, assertThrows(ApiException.class, () -> service.create(request())).getHttp());
    verifyNoInteractions(repository);
  }
}
