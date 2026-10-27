package com.mss301.inventory.service;

import com.mss301.inventory.dto.ReserveRequest;
import com.mss301.inventory.entity.*;
import com.mss301.inventory.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class InventoryServiceTest {
    private final ProductRepository products=mock(ProductRepository.class);
    private final ReservationRepository reservations=mock(ReservationRepository.class);
    private final InventoryService service=new InventoryService(products,reservations);

    @Test void duplicateReserveDoesNotDeductStockAgain() {
        Reservation r=new Reservation();r.setOrderId(1L);r.setStatus(ReservationStatus.RESERVED);
        when(reservations.findById(1L)).thenReturn(Optional.of(r));
        assertSame(r,service.reserve(new ReserveRequest(1L,"P1",2)));
        verifyNoInteractions(products);
        verify(reservations,never()).save(any());
    }
    @Test void insufficientStockDoesNotWriteEitherCollection() {
        when(products.findById("P1")).thenReturn(Optional.of(new Product("P1","Laptop",1000,1)));
        var error=assertThrows(ResponseStatusException.class,()->service.reserve(new ReserveRequest(1L,"P1",2)));
        assertEquals(HttpStatus.CONFLICT,error.getStatusCode());
        verify(products,never()).save(any());verify(reservations,never()).save(any());
    }
    @Test void unknownProductReturns404() {
        var error=assertThrows(ResponseStatusException.class,()->service.reserve(new ReserveRequest(1L,"P9",1)));
        assertEquals(HttpStatus.NOT_FOUND,error.getStatusCode());
    }
    @Test void releaseRestoresStockOnlyOnce() {
        Product p=new Product("P1","Laptop",1000,8);
        Reservation r=new Reservation();r.setOrderId(1L);r.setProductId("P1");r.setQuantity(2);r.setStatus(ReservationStatus.RESERVED);
        when(reservations.findById(1L)).thenReturn(Optional.of(r));when(products.findById("P1")).thenReturn(Optional.of(p));
        service.release(1L);service.release(1L);
        assertEquals(10,p.getStock());assertEquals(ReservationStatus.RELEASED,r.getStatus());
        verify(products,times(1)).save(p);verify(reservations,times(1)).save(r);
    }
    @Test void releaseWithoutReservationDoesNothing() {
        service.release(42L);verifyNoInteractions(products);verify(reservations,never()).save(any());
    }
}
