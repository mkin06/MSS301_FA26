package fu.de190468.user.controller;

import fu.de190468.user.common.Create;
import fu.de190468.user.dto.*;
import fu.de190468.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/api/users", produces = "application/json")
public class DE190468UserController {
  private final UserService service;

  public DE190468UserController(UserService service) {
    this.service = service;
  }

  @PostMapping(consumes = "application/json")
  public ResponseEntity<ApiResponse<UserDTO>> create(
      @Validated(Create.class) @RequestBody UserDTO dto) {
    return ResponseEntity.status(201).body(service.create(dto));
  }

  @PutMapping(value = "/{userId}", consumes = "application/json")
  public ApiResponse<UserDTO> update(@PathVariable Long userId, @Valid @RequestBody UserDTO dto) {
    return service.update(userId, dto);
  }

  @GetMapping("/{userId}")
  public ApiResponse<UserDTO> get(@PathVariable Long userId) {
    return service.get(userId);
  }

  @DeleteMapping("/{userId}")
  public ApiResponse<Void> delete(@PathVariable Long userId) {
    return service.delete(userId);
  }

  @GetMapping
  public ApiResponse<PageDTO<UserDTO>> list(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size,
      @RequestParam(required = false) String role,
      @RequestParam(required = false) String status) {
    return service.list(page, size, role, status);
  }
}
