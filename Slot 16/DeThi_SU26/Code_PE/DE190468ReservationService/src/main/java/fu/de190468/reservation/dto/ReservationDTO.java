package fu.de190468.reservation.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.OptBoolean;
import fu.de190468.reservation.common.Create;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.Date;

public class ReservationDTO {
  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  private Long reservationId;

  @NotNull(groups = Create.class)
  @Size(min = 1, max = 100)
  @Pattern(regexp = "(?s).*\\S.*")
  private String guestName;

  @NotNull(groups = Create.class)
  @Size(min = 1, max = 100)
  @Pattern(regexp = "(?s).*\\S.*")
  @Email
  private String guestEmail;

  @NotNull(groups = Create.class)
  @Size(min = 1, max = 20)
  @Pattern(regexp = "(?s).*\\S.*")
  private String guestPhone;

  @NotNull(groups = Create.class)
  @JsonFormat(
      shape = JsonFormat.Shape.STRING,
      pattern = "dd/MM/yyyy",
      timezone = "UTC",
      lenient = OptBoolean.FALSE)
  private Date checkInDate;

  @NotNull(groups = Create.class)
  @JsonFormat(
      shape = JsonFormat.Shape.STRING,
      pattern = "dd/MM/yyyy",
      timezone = "UTC",
      lenient = OptBoolean.FALSE)
  private Date checkOutDate;

  @NotNull(groups = Create.class)
  @Min(1)
  @Max(10)
  private Integer numberOfGuests;

  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  private BigDecimal totalAmount;

  @Pattern(regexp = "CONFIRMED|CHECKED_IN|CHECKED_OUT|CANCELLED")
  private String status;

  @NotNull(groups = Create.class)
  @Min(1)
  private Long roomId;

  public Long getReservationId() {
    return reservationId;
  }

  public void setReservationId(Long reservationId) {
    this.reservationId = reservationId;
  }

  public String getGuestName() {
    return guestName;
  }

  public void setGuestName(String guestName) {
    this.guestName = guestName;
  }

  public String getGuestEmail() {
    return guestEmail;
  }

  public void setGuestEmail(String guestEmail) {
    this.guestEmail = guestEmail;
  }

  public String getGuestPhone() {
    return guestPhone;
  }

  public void setGuestPhone(String guestPhone) {
    this.guestPhone = guestPhone;
  }

  public Date getCheckInDate() {
    return checkInDate;
  }

  public void setCheckInDate(Date checkInDate) {
    this.checkInDate = checkInDate;
  }

  public Date getCheckOutDate() {
    return checkOutDate;
  }

  public void setCheckOutDate(Date checkOutDate) {
    this.checkOutDate = checkOutDate;
  }

  public Integer getNumberOfGuests() {
    return numberOfGuests;
  }

  public void setNumberOfGuests(Integer numberOfGuests) {
    this.numberOfGuests = numberOfGuests;
  }

  public BigDecimal getTotalAmount() {
    return totalAmount;
  }

  public void setTotalAmount(BigDecimal totalAmount) {
    this.totalAmount = totalAmount;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public Long getRoomId() {
    return roomId;
  }

  public void setRoomId(Long roomId) {
    this.roomId = roomId;
  }
}
