package fu.de190468.post.service.impl;

import feign.FeignException;
import fu.de190468.post.client.UserClient;
import fu.de190468.post.common.*;
import fu.de190468.post.dto.*;
import fu.de190468.post.entity.*;
import fu.de190468.post.repository.PostRepository;
import java.math.BigDecimal;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PostServiceImpl implements fu.de190468.post.service.PostService {
  private final PostRepository repository;
  private final UserClient client;

  public PostServiceImpl(PostRepository repository, UserClient client) {
    this.repository = repository;
    this.client = client;
  }

  public ApiResponse<PostDTO> create(PostDTO dto) {
    validateDates(dto.getStartDate(), dto.getEndDate());
    UserDTO parent = fetch(dto.getUserId());
    validateParent(parent, dto.getNumberOfPosts());
    Post entity = new Post();
    apply(entity, dto);
    entity.setStatus(PostStatus.SCHEDULED);
    entity.setTotalCost(total(parent.getRatePerPost(), dto.getStartDate(), dto.getEndDate()));
    return ApiResponse.ok("Post created successfully", toDTO(repository.save(entity)));
  }

  public ApiResponse<PostDTO> update(Long id, PostDTO dto) {
    Post entity = find(id);
    Date start = dto.getStartDate() == null ? entity.getStartDate() : dto.getStartDate();
    Date end = dto.getEndDate() == null ? entity.getEndDate() : dto.getEndDate();
    validateDates(start, end);
    Long parentId = dto.getUserId() == null ? entity.getUserId() : dto.getUserId();
    int count = dto.getNumberOfPosts() == null ? entity.getNumberOfPosts() : dto.getNumberOfPosts();
    UserDTO parent = fetch(parentId);
    validateParent(parent, count);
    BigDecimal amount = total(parent.getRatePerPost(), start, end);
    // All business validation finishes before changing a managed entity.
    apply(entity, dto);
    entity.setTotalCost(amount);
    return ApiResponse.ok("Post updated successfully", toDTO(repository.save(entity)));
  }

  @Transactional(readOnly = true)
  public ApiResponse<PostDetailDTO> get(Long id) {
    return ApiResponse.ok("Successful", toDetail(find(id)));
  }

  public ApiResponse<Void> delete(Long id) {
    Post entity = find(id);
    entity.setStatus(PostStatus.CANCELLED);
    repository.save(entity);
    return ApiResponse.ok("Post cancelled successfully", null);
  }

  @Transactional(readOnly = true)
  public ApiResponse<PageDTO<PostDetailDTO>> list(int page, int size, String name, String status) {
    var result =
        repository.search(
            QueryOptions.contains(name),
            QueryOptions.filter(status, PostStatus.class),
            QueryOptions.page(page, size, "postId"));
    return ApiResponse.ok(
        "Successful",
        PageDTO.from(result, result.getContent().stream().map(this::toDetail).toList()));
  }

  private Post find(Long id) {
    return repository.findById(id).orElseThrow(() -> ApiException.missing("Post is not found"));
  }

  private UserDTO fetch(Long id) {
    try {
      var result = client.get(id);
      if (result == null) throw new ApiException(500, 0, "User service returned no response");
      if (result.status() == 4) throw ApiException.missing("User ID is not found");
      if (result.status() != 1 || result.data() == null)
        throw new ApiException(500, 0, "User service failed");
      return result.data();
    } catch (FeignException.NotFound ex) {
      throw ApiException.missing("User ID is not found");
    }
  }

  private void validateParent(UserDTO parent, int count) {
    if (!"ACTIVE".equals(parent.getStatus())) throw new ApiException(400, 5, "User is not ACTIVE");
    if (count > parent.getMaxDailyPosts())
      throw ApiException.invalid("numberOfPosts must not exceed maxDailyPosts");
  }

  private void validateDates(Date start, Date end) {
    if (start == null || end == null || !end.after(start))
      throw ApiException.invalid("endDate must be after startDate");
  }

  private BigDecimal total(BigDecimal rate, Date start, Date end) {
    // java.sql.Date (loaded by JPA) does not support toInstant().
    long days =
        ChronoUnit.DAYS.between(
            new Date(start.getTime()).toInstant().atZone(ZoneOffset.UTC).toLocalDate(),
            new Date(end.getTime()).toInstant().atZone(ZoneOffset.UTC).toLocalDate());
    BigDecimal amount = rate.multiply(BigDecimal.valueOf(days));
    if (amount.precision() - amount.scale() > 16)
      throw ApiException.invalid("Calculated amount exceeds decimal(18,2)");
    return amount;
  }

  private void apply(Post entity, PostDTO dto) {
    if (dto.getAdvertiserName() != null) entity.setAdvertiserName(dto.getAdvertiserName());
    if (dto.getAdvertiserEmail() != null) entity.setAdvertiserEmail(dto.getAdvertiserEmail());
    if (dto.getAdvertiserPhone() != null) entity.setAdvertiserPhone(dto.getAdvertiserPhone());
    if (dto.getStartDate() != null) entity.setStartDate(dto.getStartDate());
    if (dto.getEndDate() != null) entity.setEndDate(dto.getEndDate());
    if (dto.getNumberOfPosts() != null) entity.setNumberOfPosts(dto.getNumberOfPosts());
    if (dto.getStatus() != null) entity.setStatus(PostStatus.valueOf(dto.getStatus()));
    if (dto.getUserId() != null) entity.setUserId(dto.getUserId());
  }

  private PostDTO toDTO(Post entity) {
    PostDTO dto = new PostDTO();
    dto.setPostId(entity.getPostId());
    dto.setAdvertiserName(entity.getAdvertiserName());
    dto.setAdvertiserEmail(entity.getAdvertiserEmail());
    dto.setAdvertiserPhone(entity.getAdvertiserPhone());
    dto.setStartDate(entity.getStartDate());
    dto.setEndDate(entity.getEndDate());
    dto.setNumberOfPosts(entity.getNumberOfPosts());
    dto.setTotalCost(entity.getTotalCost());
    dto.setStatus(entity.getStatus().name());
    dto.setUserId(entity.getUserId());

    return dto;
  }

  private PostDetailDTO toDetail(Post entity) {
    PostDetailDTO dto = new PostDetailDTO();
    dto.setPostId(entity.getPostId());
    dto.setAdvertiserName(entity.getAdvertiserName());
    dto.setAdvertiserEmail(entity.getAdvertiserEmail());
    dto.setAdvertiserPhone(entity.getAdvertiserPhone());
    dto.setStartDate(entity.getStartDate());
    dto.setEndDate(entity.getEndDate());
    dto.setNumberOfPosts(entity.getNumberOfPosts());
    dto.setTotalCost(entity.getTotalCost());
    dto.setStatus(entity.getStatus().name());

    dto.setUser(fetch(entity.getUserId()));
    return dto;
  }
}
