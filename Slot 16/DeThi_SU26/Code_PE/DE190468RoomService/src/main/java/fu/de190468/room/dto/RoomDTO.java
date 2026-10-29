package fu.de190468.room.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import fu.de190468.room.common.Create;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public class RoomDTO {
  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  private Long roomId;

  @NotNull(groups = Create.class)
  @Size(min = 1, max = 20)
  @Pattern(regexp = "(?s).*\\S.*")
  private String roomNumber;

  @NotNull(groups = Create.class)
  @Pattern(regexp = "SINGLE|DOUBLE|SUITE|DELUXE")
  private String roomType;

  @NotNull(groups = Create.class)
  @DecimalMin(value = "0", inclusive = false)
  @Digits(integer = 16, fraction = 2)
  private BigDecimal pricePerNight;

  @NotNull(groups = Create.class)
  @Min(1)
  @Max(10)
  private Integer capacity;

  @NotNull(groups = Create.class)
  @Min(1)
  private Integer floor;

  @Pattern(regexp = "AVAILABLE|OCCUPIED|MAINTENANCE")
  private String status;

  public Long getRoomId() {
    return roomId;
  }

  public void setRoomId(Long roomId) {
    this.roomId = roomId;
  }

  public String getRoomNumber() {
    return roomNumber;
  }

  public void setRoomNumber(String roomNumber) {
    this.roomNumber = roomNumber;
  }

  public String getRoomType() {
    return roomType;
  }

  public void setRoomType(String roomType) {
    this.roomType = roomType;
  }

  public BigDecimal getPricePerNight() {
    return pricePerNight;
  }

  public void setPricePerNight(BigDecimal pricePerNight) {
    this.pricePerNight = pricePerNight;
  }

  public Integer getCapacity() {
    return capacity;
  }

  public void setCapacity(Integer capacity) {
    this.capacity = capacity;
  }

  public Integer getFloor() {
    return floor;
  }

  public void setFloor(Integer floor) {
    this.floor = floor;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }
}
