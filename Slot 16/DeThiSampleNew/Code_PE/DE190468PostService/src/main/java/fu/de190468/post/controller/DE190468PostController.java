package fu.de190468.post.controller;

import fu.de190468.post.common.Create;
import fu.de190468.post.dto.*;
import fu.de190468.post.service.PostService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/api/posts", produces = "application/json")
public class DE190468PostController {
  private final PostService service;

  public DE190468PostController(PostService service) {
    this.service = service;
  }

  @PostMapping(consumes = "application/json")
  public ResponseEntity<ApiResponse<PostDTO>> create(
      @Validated(Create.class) @RequestBody PostDTO dto) {
    return ResponseEntity.status(201).body(service.create(dto));
  }

  @PutMapping(value = "/{postId}", consumes = "application/json")
  public ApiResponse<PostDTO> update(@PathVariable Long postId, @Valid @RequestBody PostDTO dto) {
    return service.update(postId, dto);
  }

  @GetMapping("/{postId}")
  public ApiResponse<PostDetailDTO> get(@PathVariable Long postId) {
    return service.get(postId);
  }

  @DeleteMapping("/{postId}")
  public ApiResponse<Void> delete(@PathVariable Long postId) {
    return service.delete(postId);
  }

  @GetMapping
  public ApiResponse<PageDTO<PostDetailDTO>> list(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size,
      @RequestParam(required = false) String advertiserName,
      @RequestParam(required = false) String status) {
    return service.list(page, size, advertiserName, status);
  }
}
