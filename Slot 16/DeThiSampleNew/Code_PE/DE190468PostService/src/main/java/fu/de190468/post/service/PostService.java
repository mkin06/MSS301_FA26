package fu.de190468.post.service;

import fu.de190468.post.dto.*;

public interface PostService {
  ApiResponse<PostDTO> create(PostDTO dto);

  ApiResponse<PostDTO> update(Long id, PostDTO dto);

  ApiResponse<PostDetailDTO> get(Long id);

  ApiResponse<Void> delete(Long id);

  ApiResponse<PageDTO<PostDetailDTO>> list(int page, int size, String name, String status);
}
