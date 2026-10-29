package fu.de190468.reservation.repository;

import fu.de190468.reservation.entity.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
  @Query(
      "select e from Reservation e where (:name is null or lower(e.guestName) like lower(:name)"
          + " escape '!') and (:status is null or e.status=:status)")
  Page<Reservation> search(
      @Param("name") String name, @Param("status") ReservationStatus status, Pageable pageable);
}
