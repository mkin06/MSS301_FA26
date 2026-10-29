package fu.de190468.post.client;

import fu.de190468.post.dto.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user-service", url = "${user.service.url}")
public interface UserClient {
  @GetMapping("/api/users/{id}")
  ApiResponse<UserDTO> get(@PathVariable("id") Long id);
}
