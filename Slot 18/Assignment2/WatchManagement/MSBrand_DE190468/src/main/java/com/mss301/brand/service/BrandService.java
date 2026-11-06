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

    // [TODO-5.3] Repeated unassign is successful even if no link remains.
    @Transactional public AssignmentResponse unassignWatch(Integer watchId) {
        if(watchId==null || watchId<=0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Identifiers must be positive");
        lock.acquire(watchId);
        var link=brandWatchRepository.findByWatchId(watchId);
        if(link.isEmpty()) {
            log.info("[SAGA-PARTICIPANT] UNASSIGN watch={} -> nothing to do (idempotent)",watchId);
            return new AssignmentResponse(watchId,null,null,"NOTHING_TO_DO");
        }
        Integer previous=link.get().getBrandId();
        brandWatchRepository.delete(link.get());brandWatchRepository.flush();
        log.info("[SAGA-PARTICIPANT] UNASSIGN watch={} from brand={}",watchId,previous);
        return new AssignmentResponse(watchId,null,previous,"UNASSIGNED");
    }

    // [TODO-5.5] Queries stay within DBBrand and use deterministic ordering.
    @Transactional(readOnly=true) public List<BrandResponse> findAll() {
        return brandRepository.findAll(org.springframework.data.domain.Sort.by("brandId")).stream().map(this::toResponse).toList();
    }
    @Transactional(readOnly=true) public BrandResponse findById(Integer id) {return toResponse(getBrand(id));}
    @Transactional(readOnly=true) public boolean existsById(Integer id) {return brandRepository.existsById(id);}
    @Transactional(readOnly=true) public BrandWatchesResponse watchesOf(Integer id) {
        var brand=getBrand(id);
        var ids=brandWatchRepository.findByBrandIdOrderByWatchIdAsc(id).stream().map(BrandWatch::getWatchId).toList();
        return new BrandWatchesResponse(id,brand.getBrandName(),ids.size(),ids);
    }
    private WatchBrand getBrand(Integer id) {return brandRepository.findById(id).orElseThrow(()->missing(id));}
    private BrandResponse toResponse(WatchBrand brand) {
        return new BrandResponse(brand.getBrandId(),brand.getBrandName(),brand.getCountryOfOrigin(),brandWatchRepository.countByBrandId(brand.getBrandId()));
    }
}
