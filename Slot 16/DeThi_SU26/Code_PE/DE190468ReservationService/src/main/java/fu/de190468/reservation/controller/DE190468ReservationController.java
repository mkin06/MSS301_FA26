package fu.de190468.reservation.controller;

import fu.de190468.reservation.common.Create;
import fu.de190468.reservation.dto.*;
import fu.de190468.reservation.service.ReservationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/api/reservations", produces = "application/json")
public class DE190468ReservationController {
  private final ReservationService service;

  public DE190468ReservationController(ReservationService service) {
    this.service = service;
  }

  @PostMapping(consumes = "application/json")
  public ResponseEntity<ApiResponseDTO<ReservationDTO>> create(
      @Validated(Create.class) @RequestBody ReservationDTO dto) {
    return ResponseEntity.status(201).body(service.create(dto));
  }

  @PutMapping(value = "/{reservationId}", consumes = "application/json")
  public ApiResponseDTO<ReservationDTO> update(
      @PathVariable Long reservationId, @Valid @RequestBody ReservationDTO dto) {
    return service.update(reservationId, dto);
  }

  @GetMapping("/{reservationId}")
  public ApiResponseDTO<ReservationDetailDTO> get(@PathVariable Long reservationId) {
    return service.get(reservationId);
  }

  @DeleteMapping("/{reservationId}")
  public ApiResponseDTO<Void> delete(@PathVariable Long reservationId) {
    return service.delete(reservationId);
  }

  @GetMapping
  public ApiResponseDTO<PageDTO<ReservationDetailDTO>> list(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size,
      @RequestParam(required = false) String guestName,
      @RequestParam(required = false) String status) {
    return service.list(page, size, guestName, status);
  }
}
