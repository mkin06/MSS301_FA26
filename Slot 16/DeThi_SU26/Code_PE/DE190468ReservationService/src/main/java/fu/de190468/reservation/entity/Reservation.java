package fu.de190468.reservation.entity;

import fu.de190468.reservation.common.DateOnlyConverter;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.Date;
import org.hibernate.annotations.Nationalized;

@Entity
@Table(name = "reservations")
public class Reservation {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "reservation_id", columnDefinition = "int")
  private Long reservationId;

  @Column(name = "guest_name", nullable = false, length = 100)
  @Nationalized
  private String guestName;

  @Column(name = "guest_email", nullable = false, length = 100)
  @Nationalized
  private String guestEmail;

  @Column(name = "guest_phone", nullable = false, length = 20)
  @Nationalized
  private String guestPhone;

  @Column(name = "check_in_date", nullable = false)
  @Convert(converter = DateOnlyConverter.class)
  private Date checkInDate;

  @Column(name = "check_out_date", nullable = false)
  @Convert(converter = DateOnlyConverter.class)
  private Date checkOutDate;

  @Column(name = "number_of_guests", nullable = false)
  private Integer numberOfGuests;

  @Column(name = "total_amount", nullable = false, precision = 18, scale = 2)
  private BigDecimal totalAmount;

  @Column(name = "status", nullable = false, length = 15)
  @Enumerated(EnumType.STRING)
  private ReservationStatus status;

  @Column(name = "room_id", nullable = false)
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

  public ReservationStatus getStatus() {
    return status;
  }

  public void setStatus(ReservationStatus status) {
    this.status = status;
  }

  public Long getRoomId() {
    return roomId;
  }

  public void setRoomId(Long roomId) {
    this.roomId = roomId;
  }
}
