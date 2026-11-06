package com.mss301.brand;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.mss301.brand.entity.*;
import com.mss301.brand.repository.*;
import com.mss301.brand.service.*;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class BrandServiceTest {
  @Mock BrandRepository brands;
  @Mock BrandWatchRepository watches;
  @Mock WatchAssignmentLock lock;
  BrandService service;

  @BeforeEach
  void init() {
    service = new BrandService(brands, watches, lock);
  }

  @Test
  void missingBrandDoesNotWriteOrAcquireLock() {
    assertEquals(
        404,
        assertThrows(ResponseStatusException.class, () -> service.assignWatch(999, 99))
            .getStatusCode()
            .value());
    verifyNoInteractions(watches, lock);
  }

  @Test
  void invalidIdentifiersAreRejected() {
    assertEquals(
        400,
        assertThrows(ResponseStatusException.class, () -> service.assignWatch(0, 1))
            .getStatusCode()
            .value());
    assertEquals(
        400,
        assertThrows(ResponseStatusException.class, () -> service.unassignWatch(-1))
            .getStatusCode()
            .value());
  }

  @Test
  void assignCreatesOneLink() {
    when(brands.existsById(1)).thenReturn(true);
    var r = service.assignWatch(1, 99);
    assertEquals("ASSIGNED", r.status());
    assertNull(r.previousBrandId());
    verify(lock).acquire(99);
    var captor = ArgumentCaptor.forClass(BrandWatch.class);
    verify(watches).saveAndFlush(captor.capture());
    assertEquals(1, captor.getValue().getBrandId());
    assertEquals(99, captor.getValue().getWatchId());
  }

  @Test
  void repeatedAssignDoesNotChangeTimestampOrSave() {
    when(brands.existsById(1)).thenReturn(true);
    var link =
        BrandWatch.builder()
            .watchId(99)
            .brandId(1)
            .updatedAt(java.time.LocalDateTime.of(2026, 1, 1, 0, 0))
            .build();
    when(watches.findByWatchId(99)).thenReturn(Optional.of(link));
    service.assignWatch(1, 99);
    assertEquals(java.time.LocalDateTime.of(2026, 1, 1, 0, 0), link.getUpdatedAt());
    verify(watches, never()).saveAndFlush(any());
  }

  @Test
  void reassignmentUpdatesExistingRow() {
    when(brands.existsById(2)).thenReturn(true);
    var link = BrandWatch.builder().id(8L).watchId(99).brandId(1).build();
    when(watches.findByWatchId(99)).thenReturn(Optional.of(link));
    var r = service.assignWatch(2, 99);
    assertEquals(1, r.previousBrandId());
    assertEquals(8L, link.getId());
    assertEquals(2, link.getBrandId());
    verify(watches).saveAndFlush(link);
  }

  @Test
  void unassignRemovesLinkAndReturnsPrevious() {
    var link = BrandWatch.builder().watchId(99).brandId(2).build();
    when(watches.findByWatchId(99)).thenReturn(Optional.of(link));
    var r = service.unassignWatch(99);
    assertEquals("UNASSIGNED", r.status());
    assertEquals(2, r.previousBrandId());
    verify(watches).delete(link);
    verify(lock).acquire(99);
  }

  @Test
  void repeatedUnassignIsNoop() {
    assertEquals("NOTHING_TO_DO", service.unassignWatch(99).status());
    verify(watches, never()).delete(any());
  }

  @Test
  void detailIncludesCount() {
    when(brands.findById(1))
        .thenReturn(Optional.of(WatchBrand.builder().brandId(1).brandName("Rolex").build()));
    when(watches.countByBrandId(1)).thenReturn(2L);
    assertEquals(2, service.findById(1).watchCount());
  }

  @Test
  void watchListUsesSortedRepositoryAndPreservesIds() {
    when(brands.findById(1))
        .thenReturn(Optional.of(WatchBrand.builder().brandId(1).brandName("Rolex").build()));
    when(watches.findByBrandIdOrderByWatchIdAsc(1))
        .thenReturn(
            List.of(
                BrandWatch.builder().watchId(1).build(), BrandWatch.builder().watchId(8).build()));
    assertEquals(List.of(1, 8), service.watchesOf(1).watchIds());
  }

  @Test
  void missingBrandDetailIs404() {
    assertEquals(
        404,
        assertThrows(ResponseStatusException.class, () -> service.findById(999))
            .getStatusCode()
            .value());
  }

  @Test
  void missingBrandWatchListIs404() {
    assertEquals(
        404,
        assertThrows(ResponseStatusException.class, () -> service.watchesOf(999))
            .getStatusCode()
            .value());
  }
}
