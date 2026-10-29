package fu.de190468.reservation;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import feign.FeignException;
import feign.Request;
import feign.Response;
import fu.de190468.reservation.client.RoomClient;
import fu.de190468.reservation.common.*;
import fu.de190468.reservation.dto.*;
import fu.de190468.reservation.entity.*;
import fu.de190468.reservation.repository.ReservationRepository;
import fu.de190468.reservation.service.impl.ReservationServiceImpl;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {
  @Mock ReservationRepository repository;
  @Mock RoomClient client;
  ReservationServiceImpl service;

  @BeforeEach
  void init() {
    service = new ReservationServiceImpl(repository, client);
  }

  Date day(String value) {
    return java.sql.Date.valueOf(value);
  }

  RoomDTO parent() {
    var p = new RoomDTO();
    p.setRoomId(1L);
    p.setStatus("AVAILABLE");
    p.setPricePerNight(new BigDecimal("500000"));
    p.setCapacity(3);
    return p;
  }

  void remote(RoomDTO p) {
    when(client.get(1L)).thenReturn(ApiResponseDTO.ok("Successful", p));
  }

  ReservationDTO request() {
    var d = new ReservationDTO();
    d.setGuestName("Shop");
    d.setGuestEmail("ads@example.com");
    d.setGuestPhone("0900000000");
    d.setCheckInDate(day("2026-07-25"));
    d.setCheckOutDate(day("2026-07-28"));
    d.setNumberOfGuests(2);
    d.setRoomId(1L);
    return d;
  }

  Reservation entity() {
    var e = new Reservation();
    e.setReservationId(2L);
    e.setRoomId(1L);
    e.setCheckInDate(day("2026-07-25"));
    e.setCheckOutDate(day("2026-07-28"));
    e.setNumberOfGuests(2);
    e.setStatus(ReservationStatus.CONFIRMED);
    e.setTotalAmount(new BigDecimal("1500000"));
    return e;
  }

  @Test
  void createComputesDaysAndIgnoresClientAmountAndStatus() {
    remote(parent());
    when(repository.save(any())).thenAnswer(call -> call.getArgument(0));
    var d = request();
    d.setTotalAmount(BigDecimal.ONE);
    d.setStatus("CANCELLED");
    var result = service.create(d).data();
    assertEquals(new BigDecimal("1500000"), result.getTotalAmount());
    assertEquals("CONFIRMED", result.getStatus());
    verify(client).get(1L);
  }

  @Test
  void invalidDatesAreRejectedBeforeCallingRemoteService() {
    var d = request();
    d.setCheckOutDate(d.getCheckInDate());
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
    p.setStatus("MAINTENANCE");
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
    d.setNumberOfGuests(4);
    assertEquals(406, assertThrows(ApiException.class, () -> service.create(d)).getHttp());
    verifyNoInteractions(repository);
  }

  @Test
  void invalidPartialDatesNeverDirtyManagedEntity() {
    var e = entity();
    when(repository.findById(2L)).thenReturn(Optional.of(e));
    var d = new ReservationDTO();
    d.setCheckOutDate(day("2026-07-24"));
    assertEquals(406, assertThrows(ApiException.class, () -> service.update(2L, d)).getHttp());
    assertEquals(day("2026-07-28"), e.getCheckOutDate());
    verifyNoInteractions(client);
    verify(repository, never()).save(any());
  }

  @Test
  void partialUpdateUsesCurrentRateAndPreservesOmittedDates() {
    var e = entity();
    when(repository.findById(2L)).thenReturn(Optional.of(e));
    var p = parent();
    p.setPricePerNight(new BigDecimal("600000"));
    remote(p);
    when(repository.save(any())).thenAnswer(call -> call.getArgument(0));
    var d = new ReservationDTO();
    d.setGuestName("Updated");
    var result = service.update(2L, d).data();
    assertEquals(new BigDecimal("1800000"), result.getTotalAmount());
    assertEquals(day("2026-07-25"), result.getCheckInDate());
    assertEquals(2, result.getNumberOfGuests());
  }

  @Test
  void partialUpdateRevalidatesRemoteCapacity() {
    var e = entity();
    when(repository.findById(2L)).thenReturn(Optional.of(e));
    remote(parent());
    var d = new ReservationDTO();
    d.setNumberOfGuests(4);
    assertEquals(406, assertThrows(ApiException.class, () -> service.update(2L, d)).getHttp());
    assertEquals(2, e.getNumberOfGuests());
    verify(repository, never()).save(any());
  }

  @Test
  void partialUpdateCanChangeParentId() {
    when(repository.findById(2L)).thenReturn(Optional.of(entity()));
    when(client.get(3L)).thenReturn(ApiResponseDTO.ok("Successful", parent()));
    when(repository.save(any())).thenAnswer(call -> call.getArgument(0));
    var d = new ReservationDTO();
    d.setRoomId(3L);
    assertEquals(3L, service.update(2L, d).data().getRoomId());
    verify(client).get(3L);
  }

  @Test
  void detailEmbedsFullRemoteObject() {
    var e = entity();
    when(repository.findById(2L)).thenReturn(Optional.of(e));
    var p = parent();
    remote(p);
    assertSame(p, service.get(2L).data().getRoom());
  }

  @Test
  void deleteCancelsWithoutDeletingAndWithoutRemoteCall() {
    var e = entity();
    when(repository.findById(2L)).thenReturn(Optional.of(e));
    assertNull(service.delete(2L).data());
    assertEquals(ReservationStatus.CANCELLED, e.getStatus());
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
    p.setPricePerNight(new BigDecimal("9999999999999999.99"));
    remote(p);
    assertEquals(406, assertThrows(ApiException.class, () -> service.create(request())).getHttp());
    verifyNoInteractions(repository);
  }

  @Test
  void remoteErrorEnvelopeIsNotMisreportedAs404() {
    when(client.get(1L)).thenReturn(ApiResponseDTO.of(0, "Server error", null));
    var ex = assertThrows(ApiException.class, () -> service.create(request()));
    assertEquals(500, ex.getHttp());
    assertEquals(0, ex.getStatus());
    verifyNoInteractions(repository);
  }

  @Test
  void remoteEmptySuccessfulEnvelopeIsServerFailure() {
    when(client.get(1L)).thenReturn(ApiResponseDTO.ok("Successful", null));
    assertEquals(500, assertThrows(ApiException.class, () -> service.create(request())).getHttp());
    verifyNoInteractions(repository);
  }
}
