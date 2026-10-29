package fu.de190468.user.service;

import fu.de190468.user.dto.*;

public interface UserService {
  ApiResponse<UserDTO> create(UserDTO dto);

  ApiResponse<UserDTO> update(Long id, UserDTO dto);

  ApiResponse<UserDTO> get(Long id);

  ApiResponse<Void> delete(Long id);

  ApiResponse<PageDTO<UserDTO>> list(int page, int size, String kind, String status);
}
