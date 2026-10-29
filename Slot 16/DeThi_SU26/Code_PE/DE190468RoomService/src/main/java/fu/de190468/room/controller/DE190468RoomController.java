package fu.de190468.room.controller;

import fu.de190468.room.common.Create;
import fu.de190468.room.dto.*;
import fu.de190468.room.service.RoomService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/api/rooms", produces = "application/json")
public class DE190468RoomController {
  private final RoomService service;

  public DE190468RoomController(RoomService service) {
    this.service = service;
  }

  @PostMapping(consumes = "application/json")
  public ResponseEntity<ApiResponseDTO<RoomDTO>> create(
      @Validated(Create.class) @RequestBody RoomDTO dto) {
    return ResponseEntity.status(201).body(service.create(dto));
  }

  @PutMapping(value = "/{roomId}", consumes = "application/json")
  public ApiResponseDTO<RoomDTO> update(
      @PathVariable Long roomId, @Valid @RequestBody RoomDTO dto) {
    return service.update(roomId, dto);
  }

  @GetMapping("/{roomId}")
  public ApiResponseDTO<RoomDTO> get(@PathVariable Long roomId) {
    return service.get(roomId);
  }

  @DeleteMapping("/{roomId}")
  public ApiResponseDTO<Void> delete(@PathVariable Long roomId) {
    return service.delete(roomId);
  }

  @GetMapping
  public ApiResponseDTO<PageDTO<RoomDTO>> list(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size,
      @RequestParam(required = false) String roomType,
      @RequestParam(required = false) String status) {
    return service.list(page, size, roomType, status);
  }
}
