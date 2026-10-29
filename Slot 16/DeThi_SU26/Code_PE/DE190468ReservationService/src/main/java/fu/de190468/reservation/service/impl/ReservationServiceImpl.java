package fu.de190468.reservation.service.impl;

import feign.FeignException;
import fu.de190468.reservation.client.RoomClient;
import fu.de190468.reservation.common.*;
import fu.de190468.reservation.dto.*;
import fu.de190468.reservation.entity.*;
import fu.de190468.reservation.repository.ReservationRepository;
import java.math.BigDecimal;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ReservationServiceImpl implements fu.de190468.reservation.service.ReservationService {
  private final ReservationRepository repository;
  private final RoomClient client;

  public ReservationServiceImpl(ReservationRepository repository, RoomClient client) {
    this.repository = repository;
    this.client = client;
  }

  public ApiResponseDTO<ReservationDTO> create(ReservationDTO dto) {
    validateDates(dto.getCheckInDate(), dto.getCheckOutDate());
    RoomDTO parent = fetch(dto.getRoomId());
    validateParent(parent, dto.getNumberOfGuests());
    Reservation entity = new Reservation();
    apply(entity, dto);
    entity.setStatus(ReservationStatus.CONFIRMED);
    entity.setTotalAmount(
        total(parent.getPricePerNight(), dto.getCheckInDate(), dto.getCheckOutDate()));
    return ApiResponseDTO.ok("Reservation created successfully", toDTO(repository.save(entity)));
  }

  public ApiResponseDTO<ReservationDTO> update(Long id, ReservationDTO dto) {
    Reservation entity = find(id);
    Date start = dto.getCheckInDate() == null ? entity.getCheckInDate() : dto.getCheckInDate();
    Date end = dto.getCheckOutDate() == null ? entity.getCheckOutDate() : dto.getCheckOutDate();
    validateDates(start, end);
    Long parentId = dto.getRoomId() == null ? entity.getRoomId() : dto.getRoomId();
    int count =
        dto.getNumberOfGuests() == null ? entity.getNumberOfGuests() : dto.getNumberOfGuests();
    RoomDTO parent = fetch(parentId);
    validateParent(parent, count);
    BigDecimal amount = total(parent.getPricePerNight(), start, end);
    // All business validation finishes before changing a managed entity.
    apply(entity, dto);
    entity.setTotalAmount(amount);
    return ApiResponseDTO.ok("Reservation updated successfully", toDTO(repository.save(entity)));
  }

  @Transactional(readOnly = true)
  public ApiResponseDTO<ReservationDetailDTO> get(Long id) {
    return ApiResponseDTO.ok("Successful", toDetail(find(id)));
  }

  public ApiResponseDTO<Void> delete(Long id) {
    Reservation entity = find(id);
    entity.setStatus(ReservationStatus.CANCELLED);
    repository.save(entity);
    return ApiResponseDTO.ok("Reservation cancelled successfully", null);
  }

  @Transactional(readOnly = true)
  public ApiResponseDTO<PageDTO<ReservationDetailDTO>> list(
      int page, int size, String name, String status) {
    var result =
        repository.search(
            QueryOptions.contains(name),
            QueryOptions.filter(status, ReservationStatus.class),
            QueryOptions.page(page, size, "reservationId"));
    return ApiResponseDTO.ok(
        "Successful",
        PageDTO.from(result, result.getContent().stream().map(this::toDetail).toList()));
  }

  private Reservation find(Long id) {
    return repository
        .findById(id)
        .orElseThrow(() -> ApiException.missing("Reservation is not found"));
  }

  private RoomDTO fetch(Long id) {
    try {
      var result = client.get(id);
      if (result == null) throw new ApiException(500, 0, "Room service returned no response");
      if (result.status() == 4) throw ApiException.missing("Room ID is not found");
      if (result.status() != 1 || result.data() == null)
        throw new ApiException(500, 0, "Room service failed");
      return result.data();
    } catch (FeignException.NotFound ex) {
      throw ApiException.missing("Room ID is not found");
    }
  }

  private void validateParent(RoomDTO parent, int count) {
    if (!"AVAILABLE".equals(parent.getStatus()))
      throw new ApiException(400, 5, "Room is not AVAILABLE");
    if (count > parent.getCapacity())
      throw ApiException.invalid("numberOfGuests must not exceed capacity");
  }

  private void validateDates(Date start, Date end) {
    if (start == null || end == null || !end.after(start))
      throw ApiException.invalid("checkOutDate must be after checkInDate");
  }

  private BigDecimal total(BigDecimal rate, Date start, Date end) {
    // java.sql.Date (loaded by JPA) does not support toInstant().
    long days =
        ChronoUnit.DAYS.between(
            new Date(start.getTime()).toInstant().atZone(ZoneOffset.UTC).toLocalDate(),
            new Date(end.getTime()).toInstant().atZone(ZoneOffset.UTC).toLocalDate());
    BigDecimal amount = rate.multiply(BigDecimal.valueOf(days));
    if (amount.precision() - amount.scale() > 16)
      throw ApiException.invalid("Calculated amount exceeds decimal(18,2)");
    return amount;
  }

  private void apply(Reservation entity, ReservationDTO dto) {
    if (dto.getGuestName() != null) entity.setGuestName(dto.getGuestName());
    if (dto.getGuestEmail() != null) entity.setGuestEmail(dto.getGuestEmail());
    if (dto.getGuestPhone() != null) entity.setGuestPhone(dto.getGuestPhone());
    if (dto.getCheckInDate() != null) entity.setCheckInDate(dto.getCheckInDate());
    if (dto.getCheckOutDate() != null) entity.setCheckOutDate(dto.getCheckOutDate());
    if (dto.getNumberOfGuests() != null) entity.setNumberOfGuests(dto.getNumberOfGuests());
    if (dto.getStatus() != null) entity.setStatus(ReservationStatus.valueOf(dto.getStatus()));
    if (dto.getRoomId() != null) entity.setRoomId(dto.getRoomId());
  }

  private ReservationDTO toDTO(Reservation entity) {
    ReservationDTO dto = new ReservationDTO();
    dto.setReservationId(entity.getReservationId());
    dto.setGuestName(entity.getGuestName());
    dto.setGuestEmail(entity.getGuestEmail());
    dto.setGuestPhone(entity.getGuestPhone());
    dto.setCheckInDate(entity.getCheckInDate());
    dto.setCheckOutDate(entity.getCheckOutDate());
    dto.setNumberOfGuests(entity.getNumberOfGuests());
    dto.setTotalAmount(entity.getTotalAmount());
    dto.setStatus(entity.getStatus().name());
    dto.setRoomId(entity.getRoomId());

    return dto;
  }

  private ReservationDetailDTO toDetail(Reservation entity) {
    ReservationDetailDTO dto = new ReservationDetailDTO();
    dto.setReservationId(entity.getReservationId());
    dto.setGuestName(entity.getGuestName());
    dto.setGuestEmail(entity.getGuestEmail());
    dto.setGuestPhone(entity.getGuestPhone());
    dto.setCheckInDate(entity.getCheckInDate());
    dto.setCheckOutDate(entity.getCheckOutDate());
    dto.setNumberOfGuests(entity.getNumberOfGuests());
    dto.setTotalAmount(entity.getTotalAmount());
    dto.setStatus(entity.getStatus().name());

    dto.setRoom(fetch(entity.getRoomId()));
    return dto;
  }
}
