package fu.de190468.room;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import fu.de190468.room.common.*;
import fu.de190468.room.dto.*;
import fu.de190468.room.entity.*;
import fu.de190468.room.repository.RoomRepository;
import fu.de190468.room.service.impl.RoomServiceImpl;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class RoomServiceTest {
  @Mock RoomRepository repository;
  RoomServiceImpl service;

  @BeforeEach
  void init() {
    service = new RoomServiceImpl(repository);
  }

  RoomDTO request() {
    var d = new RoomDTO();
    d.setRoomNumber("unique");
    d.setRoomType("DOUBLE");
    d.setPricePerNight(new BigDecimal("500000"));
    d.setCapacity(2);
    d.setFloor(1);
    return d;
  }

  Room entity() {
    var e = new Room();
    e.setRoomId(1L);
    e.setRoomNumber("original");
    e.setRoomType(RoomType.DOUBLE);
    e.setPricePerNight(new BigDecimal("500000"));
    e.setCapacity(2);
    e.setFloor(1);
    e.setStatus(RoomStatus.AVAILABLE);
    return e;
  }

  @Test
  void createSetsDefaultStatusAndReturnsGeneratedId() {
    when(repository.saveAndFlush(any()))
        .thenAnswer(
            call -> {
              Room e = call.getArgument(0);
              e.setRoomId(1L);
              return e;
            });
    var result = service.create(request());
    assertEquals(1, result.status());
    assertEquals(1L, result.data().getRoomId());
    assertEquals("AVAILABLE", result.data().getStatus());
  }

  @Test
  void duplicateCreateReturns226WithoutSaving() {
    when(repository.existsByRoomNumber("unique")).thenReturn(true);
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
    var d = new RoomDTO();
    d.setFloor(3);
    var result = service.update(1L, d).data();
    assertEquals(3, result.getFloor());
    assertEquals("original", result.getRoomNumber());
    assertEquals(new BigDecimal("500000"), result.getPricePerNight());
  }

  @Test
  void updateExcludesItsOwnIdFromDuplicateCheck() {
    when(repository.findById(1L)).thenReturn(Optional.of(entity()));
    when(repository.saveAndFlush(any())).thenAnswer(call -> call.getArgument(0));
    var d = new RoomDTO();
    d.setRoomNumber("original");
    service.update(1L, d);
    verify(repository).existsByRoomNumberAndRoomIdNot("original", 1L);
  }

  @Test
  void duplicateUpdateDoesNotMutateEntity() {
    var e = entity();
    when(repository.findById(1L)).thenReturn(Optional.of(e));
    when(repository.existsByRoomNumberAndRoomIdNot("unique", 1L)).thenReturn(true);
    assertEquals(
        226, assertThrows(ApiException.class, () -> service.update(1L, request())).getHttp());
    assertEquals("original", e.getRoomNumber());
  }

  @Test
  void allMissingOperationsReturn404() {
    assertEquals(404, assertThrows(ApiException.class, () -> service.get(99L)).getHttp());
    assertEquals(
        404, assertThrows(ApiException.class, () -> service.update(99L, new RoomDTO())).getHttp());
    assertEquals(404, assertThrows(ApiException.class, () -> service.delete(99L)).getHttp());
  }

  @Test
  void deleteIsSoftAndDoesNotRemoveRow() {
    var e = entity();
    when(repository.findById(1L)).thenReturn(Optional.of(e));
    var result = service.delete(1L);
    assertEquals(RoomStatus.MAINTENANCE, e.getStatus());
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
