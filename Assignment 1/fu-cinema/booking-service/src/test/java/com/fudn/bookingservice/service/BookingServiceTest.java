package com.fudn.bookingservice.service;

import com.fudn.bookingservice.client.MovieClient;
import com.fudn.bookingservice.dto.*;
import com.fudn.bookingservice.exception.ApiException;
import com.fudn.bookingservice.model.*;
import com.fudn.bookingservice.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BookingServiceTest {
    private final BookingRepository bookings = mock(BookingRepository.class);
    private final BookingDetailRepository details = mock(BookingDetailRepository.class);
    private final MovieClient movies = mock(MovieClient.class);
    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private BookingService service;

    @BeforeEach
    void setUp() {
        service = new BookingService(bookings, details, movies, jdbc);
    }

    private Booking booking(LocalDateTime start) {
        Booking b = new Booking();
        b.setBookingId(1L);
        b.setCustomerId(10L);
        b.setBookingStatus(BookingStatus.CONFIRMED);
        b.setBookingDate(LocalDateTime.now());
        b.setTotalPrice(new BigDecimal("95000"));
        BookingDetail d = new BookingDetail();
        d.setShowtimeStart(start);
        d.setShowtimeId("66f300000000000000000001");
        d.setSeatCode("A1");
        d.setMovieId("66f200000000000000000001");
        d.setMovieTitle("Test movie");
        d.setPrice(new BigDecimal("95000"));
        b.addDetail(d);
        when(bookings.findById(1L)).thenReturn(Optional.of(b));
        when(bookings.save(any())).thenAnswer(i -> i.getArgument(0));
        return b;
    }

    private void rejects(HttpStatus status, Runnable action) {
        assertEquals(status, assertThrows(ApiException.class, action::run).getStatus());
    }

    @Test void otherCustomerCannotRead() {
        booking(LocalDateTime.now().plusDays(1));
        rejects(HttpStatus.FORBIDDEN, () -> service.getById(1L, 11L, "CUSTOMER"));
    }
    @Test void ownerAndAdminCanRead() {
        booking(LocalDateTime.now().plusDays(1));
        assertEquals(1L, service.getById(1L, 10L, "CUSTOMER").bookingId());
        assertEquals(1L, service.getById(1L, 0L, "ADMIN").bookingId());
    }
    @Test void otherCustomerCannotCancel() {
        booking(LocalDateTime.now().plusDays(1));
        rejects(HttpStatus.FORBIDDEN, () -> service.cancel(1L, 11L, "CUSTOMER"));
        verify(bookings, never()).save(any());
    }
    @Test void customerCannotCancelWithinTwoHours() {
        booking(LocalDateTime.now().plusMinutes(119));
        rejects(HttpStatus.BAD_REQUEST, () -> service.cancel(1L, 10L, "CUSTOMER"));
    }
    @Test void customerCanCancelBeforeDeadline() {
        booking(LocalDateTime.now().plusMinutes(121));
        assertEquals(BookingStatus.CANCELLED, service.cancel(1L, 10L, "CUSTOMER").bookingStatus());
        verify(jdbc).update("DELETE FROM active_seat WHERE booking_id = ?", 1L);
    }
    @Test void adminCanCancelAfterStart() {
        booking(LocalDateTime.now().minusHours(1));
        assertEquals(BookingStatus.CANCELLED, service.cancel(1L, 0L, "ADMIN").bookingStatus());
    }
    @Test void cannotCancelTwice() {
        booking(LocalDateTime.now().plusDays(1)).setBookingStatus(BookingStatus.CANCELLED);
        rejects(HttpStatus.BAD_REQUEST, () -> service.cancel(1L, 10L, "CUSTOMER"));
    }
    @Test void reportRejectsReversedDates() {
        rejects(HttpStatus.BAD_REQUEST, () -> service.report(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 1, 1)));
        verifyNoInteractions(bookings);
    }
    @Test void reportUsesExclusiveNextDay() {
        LocalDate day = LocalDate.of(2026, 10, 7);
        when(bookings.findForReport(BookingStatus.CONFIRMED, day.atStartOfDay(), day.plusDays(1).atStartOfDay())).thenReturn(List.of());
        ReportResponse report = service.report(day, day);
        assertEquals(0, report.totalBookings());
        assertEquals(BigDecimal.ZERO, report.totalRevenue());
        verify(bookings).findForReport(BookingStatus.CONFIRMED, day.atStartOfDay(), day.plusDays(1).atStartOfDay());
    }
}
