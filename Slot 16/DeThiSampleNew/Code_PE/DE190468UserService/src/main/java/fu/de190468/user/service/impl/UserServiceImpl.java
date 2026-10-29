package fu.de190468.user.service.impl;

import fu.de190468.user.common.*;
import fu.de190468.user.dto.*;
import fu.de190468.user.entity.*;
import fu.de190468.user.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UserServiceImpl implements fu.de190468.user.service.UserService {
  private final UserRepository repository;

  public UserServiceImpl(UserRepository repository) {
    this.repository = repository;
  }

  public ApiResponse<UserDTO> create(UserDTO dto) {
    if (repository.existsByUsername(dto.getUsername())) throw duplicate();
    User entity = new User();
    entity.setStatus(UserStatus.ACTIVE);
    apply(entity, dto);
    return ApiResponse.ok("User created successfully", toDTO(save(entity)));
  }

  public ApiResponse<UserDTO> update(Long id, UserDTO dto) {
    User entity = find(id);
    if (dto.getUsername() != null && repository.existsByUsernameAndUserIdNot(dto.getUsername(), id))
      throw duplicate();
    apply(entity, dto);
    return ApiResponse.ok("User updated successfully", toDTO(save(entity)));
  }

  @Transactional(readOnly = true)
  public ApiResponse<UserDTO> get(Long id) {
    return ApiResponse.ok("Successful", toDTO(find(id)));
  }

  public ApiResponse<Void> delete(Long id) {
    User entity = find(id);
    entity.setStatus(UserStatus.DEACTIVATED);
    repository.save(entity);
    return ApiResponse.ok("User is set to DEACTIVATED successfully", null);
  }

  @Transactional(readOnly = true)
  public ApiResponse<PageDTO<UserDTO>> list(int page, int size, String kind, String status) {
    var result =
        repository.search(
            QueryOptions.filter(kind, Role.class),
            QueryOptions.filter(status, UserStatus.class),
            QueryOptions.page(page, size, "userId"));
    return ApiResponse.ok(
        "Successful", PageDTO.from(result, result.getContent().stream().map(this::toDTO).toList()));
  }

  private User find(Long id) {
    return repository.findById(id).orElseThrow(() -> ApiException.missing("User is not found"));
  }

  private ApiException duplicate() {
    return new ApiException(226, 3, "username is duplicated");
  }

  private User save(User entity) {
    try {
      return repository.saveAndFlush(entity);
    } catch (DataIntegrityViolationException ex) {
      throw duplicate();
    }
  }

  private void apply(User entity, UserDTO dto) {
    if (dto.getUsername() != null) entity.setUsername(dto.getUsername());
    if (dto.getEmail() != null) entity.setEmail(dto.getEmail());
    if (dto.getRole() != null) entity.setRole(Role.valueOf(dto.getRole()));
    if (dto.getRatePerPost() != null) entity.setRatePerPost(dto.getRatePerPost());
    if (dto.getMaxDailyPosts() != null) entity.setMaxDailyPosts(dto.getMaxDailyPosts());
    if (dto.getLevel() != null) entity.setLevel(dto.getLevel());
    if (dto.getStatus() != null) entity.setStatus(UserStatus.valueOf(dto.getStatus()));
  }

  private UserDTO toDTO(User entity) {
    UserDTO dto = new UserDTO();
    dto.setUserId(entity.getUserId());
    dto.setUsername(entity.getUsername());
    dto.setEmail(entity.getEmail());
    dto.setRole(entity.getRole().name());
    dto.setRatePerPost(entity.getRatePerPost());
    dto.setMaxDailyPosts(entity.getMaxDailyPosts());
    dto.setLevel(entity.getLevel());
    dto.setStatus(entity.getStatus().name());

    return dto;
  }
}
