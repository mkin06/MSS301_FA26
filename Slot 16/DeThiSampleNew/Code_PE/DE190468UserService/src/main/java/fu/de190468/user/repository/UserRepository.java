package fu.de190468.user.repository;

import fu.de190468.user.entity.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {
  boolean existsByUsername(String value);

  boolean existsByUsernameAndUserIdNot(String value, Long id);

  @Query(
      "select e from User e where (:kind is null or e.role=:kind) and (:status is null or"
          + " e.status=:status)")
  Page<User> search(
      @Param("kind") Role kind, @Param("status") UserStatus status, Pageable pageable);
}
