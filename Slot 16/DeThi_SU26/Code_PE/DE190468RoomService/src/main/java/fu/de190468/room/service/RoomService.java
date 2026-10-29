package fu.de190468.room.service;

import fu.de190468.room.dto.*;

public interface RoomService {
  ApiResponseDTO<RoomDTO> create(RoomDTO dto);

  ApiResponseDTO<RoomDTO> update(Long id, RoomDTO dto);

  ApiResponseDTO<RoomDTO> get(Long id);

  ApiResponseDTO<Void> delete(Long id);

  ApiResponseDTO<PageDTO<RoomDTO>> list(int page, int size, String kind, String status);
}
