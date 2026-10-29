package fu.de190468.reservation.client;

import fu.de190468.reservation.dto.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "room-service", url = "${room.service.url}")
public interface RoomClient {
  @GetMapping("/api/rooms/{id}")
  ApiResponseDTO<RoomDTO> get(@PathVariable("id") Long id);
}
