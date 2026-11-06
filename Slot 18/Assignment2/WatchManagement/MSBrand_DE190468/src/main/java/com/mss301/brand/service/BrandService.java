package com.mss301.brand.service;
import com.mss301.brand.dto.*;
import com.mss301.brand.entity.*;
import com.mss301.brand.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDateTime;
import java.util.List;
@Service @RequiredArgsConstructor @Slf4j
public class BrandService {
    private final BrandRepository brandRepository;
    private final BrandWatchRepository brandWatchRepository;
    private final WatchAssignmentLock lock;
    // [TODO-5.2] Idempotent state upsert, one local transaction in DBBrand.
    @Transactional public AssignmentResponse assignWatch(Integer brandId,Integer watchId) {
        if(brandId==null || brandId<=0 || watchId==null || watchId<=0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Identifiers must be positive");
        if(!brandRepository.existsById(brandId)) throw missing(brandId);
        lock.acquire(watchId);
        var link=brandWatchRepository.findByWatchId(watchId).orElseGet(()->BrandWatch.builder().watchId(watchId).build());
        Integer previous=link.getBrandId();
        if(!brandId.equals(previous)) {
            link.setBrandId(brandId);link.setUpdatedAt(LocalDateTime.now());brandWatchRepository.saveAndFlush(link);
        }
        log.info("[SAGA-PARTICIPANT] ASSIGN watch={} -> brand={} (previous brand={})",watchId,brandId,previous);
        return new AssignmentResponse(watchId,brandId,previous,"ASSIGNED");
    }
    private ResponseStatusException missing(Integer id) {return new ResponseStatusException(HttpStatus.NOT_FOUND,"Brand not found with id: "+id);}
}
