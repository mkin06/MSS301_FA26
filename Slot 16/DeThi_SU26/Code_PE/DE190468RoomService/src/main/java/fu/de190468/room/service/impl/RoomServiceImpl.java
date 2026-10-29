package fu.de190468.room.service.impl;

import fu.de190468.room.common.*;
import fu.de190468.room.dto.*;
import fu.de190468.room.entity.*;
import fu.de190468.room.repository.RoomRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class RoomServiceImpl implements fu.de190468.room.service.RoomService {
  private final RoomRepository repository;

  public RoomServiceImpl(RoomRepository repository) {
    this.repository = repository;
  }

  public ApiResponseDTO<RoomDTO> create(RoomDTO dto) {
    if (repository.existsByRoomNumber(dto.getRoomNumber())) throw duplicate();
    Room entity = new Room();
    entity.setStatus(RoomStatus.AVAILABLE);
    apply(entity, dto);
    return ApiResponseDTO.ok("Room created successfully", toDTO(save(entity)));
  }

  public ApiResponseDTO<RoomDTO> update(Long id, RoomDTO dto) {
    Room entity = find(id);
    if (dto.getRoomNumber() != null
        && repository.existsByRoomNumberAndRoomIdNot(dto.getRoomNumber(), id)) throw duplicate();
    apply(entity, dto);
    return ApiResponseDTO.ok("Room updated successfully", toDTO(save(entity)));
  }

  @Transactional(readOnly = true)
  public ApiResponseDTO<RoomDTO> get(Long id) {
    return ApiResponseDTO.ok("Successful", toDTO(find(id)));
  }

  public ApiResponseDTO<Void> delete(Long id) {
    Room entity = find(id);
    entity.setStatus(RoomStatus.MAINTENANCE);
    repository.save(entity);
    return ApiResponseDTO.ok("Room is set to MAINTENANCE successfully", null);
  }

  @Transactional(readOnly = true)
  public ApiResponseDTO<PageDTO<RoomDTO>> list(int page, int size, String kind, String status) {
    var result =
        repository.search(
            QueryOptions.filter(kind, RoomType.class),
            QueryOptions.filter(status, RoomStatus.class),
            QueryOptions.page(page, size, "roomId"));
    return ApiResponseDTO.ok(
        "Successful", PageDTO.from(result, result.getContent().stream().map(this::toDTO).toList()));
  }

  private Room find(Long id) {
    return repository.findById(id).orElseThrow(() -> ApiException.missing("Room is not found"));
  }

  private ApiException duplicate() {
    return new ApiException(226, 3, "roomNumber is duplicated");
  }

  private Room save(Room entity) {
    try {
      return repository.saveAndFlush(entity);
    } catch (DataIntegrityViolationException ex) {
      throw duplicate();
    }
  }

  private void apply(Room entity, RoomDTO dto) {
    if (dto.getRoomNumber() != null) entity.setRoomNumber(dto.getRoomNumber());
    if (dto.getRoomType() != null) entity.setRoomType(RoomType.valueOf(dto.getRoomType()));
    if (dto.getPricePerNight() != null) entity.setPricePerNight(dto.getPricePerNight());
    if (dto.getCapacity() != null) entity.setCapacity(dto.getCapacity());
    if (dto.getFloor() != null) entity.setFloor(dto.getFloor());
    if (dto.getStatus() != null) entity.setStatus(RoomStatus.valueOf(dto.getStatus()));
  }

  private RoomDTO toDTO(Room entity) {
    RoomDTO dto = new RoomDTO();
    dto.setRoomId(entity.getRoomId());
    dto.setRoomNumber(entity.getRoomNumber());
    dto.setRoomType(entity.getRoomType().name());
    dto.setPricePerNight(entity.getPricePerNight());
    dto.setCapacity(entity.getCapacity());
    dto.setFloor(entity.getFloor());
    dto.setStatus(entity.getStatus().name());

    return dto;
  }
}
