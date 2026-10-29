package fu.de190468.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import fu.de190468.user.common.Create;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public class UserDTO {
  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  private Long userId;

  @NotNull(groups = Create.class)
  @Size(min = 1, max = 30)
  @Pattern(regexp = "(?s).*\\S.*")
  private String username;

  @NotNull(groups = Create.class)
  @Email
  @Size(min = 1, max = 100)
  @Pattern(regexp = "(?s).*\\S.*")
  private String email;

  @NotNull(groups = Create.class)
  @Pattern(regexp = "USER|CREATOR|ADMIN")
  private String role;

  @NotNull(groups = Create.class)
  @DecimalMin(value = "0", inclusive = false)
  @Digits(integer = 16, fraction = 2)
  private BigDecimal ratePerPost;

  @NotNull(groups = Create.class)
  @Min(1)
  @Max(10)
  private Integer maxDailyPosts;

  @NotNull(groups = Create.class)
  @Min(1)
  private Integer level;

  @Pattern(regexp = "ACTIVE|SUSPENDED|DEACTIVATED")
  private String status;

  public Long getUserId() {
    return userId;
  }

  public void setUserId(Long userId) {
    this.userId = userId;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getRole() {
    return role;
  }

  public void setRole(String role) {
    this.role = role;
  }

  public BigDecimal getRatePerPost() {
    return ratePerPost;
  }

  public void setRatePerPost(BigDecimal ratePerPost) {
    this.ratePerPost = ratePerPost;
  }

  public Integer getMaxDailyPosts() {
    return maxDailyPosts;
  }

  public void setMaxDailyPosts(Integer maxDailyPosts) {
    this.maxDailyPosts = maxDailyPosts;
  }

  public Integer getLevel() {
    return level;
  }

  public void setLevel(Integer level) {
    this.level = level;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }
}
