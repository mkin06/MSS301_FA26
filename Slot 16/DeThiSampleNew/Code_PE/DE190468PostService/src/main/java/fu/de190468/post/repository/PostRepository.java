package fu.de190468.post.repository;

import fu.de190468.post.entity.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostRepository extends JpaRepository<Post, Long> {
  @Query(
      "select e from Post e where (:name is null or lower(e.advertiserName) like lower(:name)"
          + " escape '!') and (:status is null or e.status=:status)")
  Page<Post> search(
      @Param("name") String name, @Param("status") PostStatus status, Pageable pageable);
}
