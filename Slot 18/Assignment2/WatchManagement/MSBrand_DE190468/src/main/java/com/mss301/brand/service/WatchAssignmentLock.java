package com.mss301.brand.service;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
// [TODO-5.2, TODO-5.3] SQL transaction-owned mutex includes the missing-row case.
// A UNIQUE constraint alone would make simultaneous first assigns race.
@Component @RequiredArgsConstructor public class WatchAssignmentLock {
    private final JdbcTemplate jdbc;
    public void acquire(Integer watchId) {
        Integer code=jdbc.queryForObject("""
            DECLARE @result int;
            EXEC @result = sys.sp_getapplock @Resource=?, @LockMode='Exclusive',
                @LockOwner='Transaction', @LockTimeout=5000;
            SELECT @result;
            """,Integer.class,"BrandWatches:"+watchId);
        if(code==null || code<0) throw new IllegalStateException("Cannot acquire watch assignment lock");
    }
}
