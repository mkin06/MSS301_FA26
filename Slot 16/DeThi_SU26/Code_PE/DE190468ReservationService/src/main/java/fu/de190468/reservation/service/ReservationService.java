package fu.de190468.reservation.service;

import fu.de190468.reservation.dto.*;

public interface ReservationService {
  ApiResponseDTO<ReservationDTO> create(ReservationDTO dto);

  ApiResponseDTO<ReservationDTO> update(Long id, ReservationDTO dto);

  ApiResponseDTO<ReservationDetailDTO> get(Long id);

  ApiResponseDTO<Void> delete(Long id);

  ApiResponseDTO<PageDTO<ReservationDetailDTO>> list(
      int page, int size, String name, String status);
}
