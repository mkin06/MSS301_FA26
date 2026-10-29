package fu.de190468.post.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.OptBoolean;
import fu.de190468.post.common.Create;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.Date;

public class PostDTO {
  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  private Long postId;

  @NotNull(groups = Create.class)
  @Size(min = 1, max = 100)
  @Pattern(regexp = "(?s).*\\S.*")
  private String advertiserName;

  @NotNull(groups = Create.class)
  @Size(min = 1, max = 100)
  @Pattern(regexp = "(?s).*\\S.*")
  @Email
  private String advertiserEmail;

  @NotNull(groups = Create.class)
  @Size(min = 1, max = 20)
  @Pattern(regexp = "(?s).*\\S.*")
  private String advertiserPhone;

  @NotNull(groups = Create.class)
  @JsonFormat(
      shape = JsonFormat.Shape.STRING,
      pattern = "dd/MM/yyyy",
      timezone = "UTC",
      lenient = OptBoolean.FALSE)
  private Date startDate;

  @NotNull(groups = Create.class)
  @JsonFormat(
      shape = JsonFormat.Shape.STRING,
      pattern = "dd/MM/yyyy",
      timezone = "UTC",
      lenient = OptBoolean.FALSE)
  private Date endDate;

  @NotNull(groups = Create.class)
  @Min(1)
  private Integer numberOfPosts;

  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  private BigDecimal totalCost;

  @Pattern(regexp = "DRAFT|SCHEDULED|PUBLISHED|CANCELLED")
  private String status;

  @NotNull(groups = Create.class)
  @Min(1)
  private Long userId;

  public Long getPostId() {
    return postId;
  }

  public void setPostId(Long postId) {
    this.postId = postId;
  }

  public String getAdvertiserName() {
    return advertiserName;
  }

  public void setAdvertiserName(String advertiserName) {
    this.advertiserName = advertiserName;
  }

  public String getAdvertiserEmail() {
    return advertiserEmail;
  }

  public void setAdvertiserEmail(String advertiserEmail) {
    this.advertiserEmail = advertiserEmail;
  }

  public String getAdvertiserPhone() {
    return advertiserPhone;
  }

  public void setAdvertiserPhone(String advertiserPhone) {
    this.advertiserPhone = advertiserPhone;
  }

  public Date getStartDate() {
    return startDate;
  }

  public void setStartDate(Date startDate) {
    this.startDate = startDate;
  }

  public Date getEndDate() {
    return endDate;
  }

  public void setEndDate(Date endDate) {
    this.endDate = endDate;
  }

  public Integer getNumberOfPosts() {
    return numberOfPosts;
  }

  public void setNumberOfPosts(Integer numberOfPosts) {
    this.numberOfPosts = numberOfPosts;
  }

  public BigDecimal getTotalCost() {
    return totalCost;
  }

  public void setTotalCost(BigDecimal totalCost) {
    this.totalCost = totalCost;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public Long getUserId() {
    return userId;
  }

  public void setUserId(Long userId) {
    this.userId = userId;
  }
}
