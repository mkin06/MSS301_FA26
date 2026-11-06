package com.mss301.brand.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

// [TODO-5.2, TODO-5.3] SQL transaction-owned mutex includes the missing-row case.
// A UNIQUE constraint alone would make simultaneous first assigns race.
@Component
@RequiredArgsConstructor
public class WatchAssignmentLock {
  private final JdbcTemplate jdbc;

  @Transactional(propagation = Propagation.MANDATORY)
  public void acquire(Integer watchId) {
    // SQL Server starts the physical JDBC transaction lazily. A table read must
    // precede sp_getapplock even when no assignment row exists (unassign retry).
    jdbc.queryForObject("SELECT COUNT_BIG(*) FROM BrandWatches WHERE 1=0", Long.class);
    Integer code =
        jdbc.queryForObject(
            """
            DECLARE @result int;
            EXEC @result = sys.sp_getapplock @Resource=?, @LockMode='Exclusive',
                @LockOwner='Transaction', @LockTimeout=5000;
            SELECT @result;
            """,
            Integer.class,
            "BrandWatches:" + watchId);
    if (code == null || code < 0)
      throw new IllegalStateException("Cannot acquire watch assignment lock");
  }
}
