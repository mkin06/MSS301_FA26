package fu.de190468.room.repository;

import fu.de190468.room.entity.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoomRepository extends JpaRepository<Room, Long> {
  boolean existsByRoomNumber(String value);

  boolean existsByRoomNumberAndRoomIdNot(String value, Long id);

  @Query(
      "select e from Room e where (:kind is null or e.roomType=:kind) and (:status is null or"
          + " e.status=:status)")
  Page<Room> search(
      @Param("kind") RoomType kind, @Param("status") RoomStatus status, Pageable pageable);
}
