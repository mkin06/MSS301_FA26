-- A primary key reserves each seat once, including concurrent transactions.
-- Cancelled tickets remain in booking_detail; their reservations are removed.
CREATE TABLE active_seat (
    showtime_id VARCHAR(24) NOT NULL,
    seat_code VARCHAR(4) NOT NULL,
    booking_id BIGINT NOT NULL,
    PRIMARY KEY (showtime_id, seat_code),
    CONSTRAINT fk_active_seat_booking FOREIGN KEY (booking_id)
        REFERENCES booking(booking_id),
    INDEX idx_active_seat_booking (booking_id)
);

INSERT INTO active_seat (showtime_id, seat_code, booking_id)
SELECT d.showtime_id, d.seat_code, d.booking_id
FROM booking_detail d JOIN booking b ON b.booking_id = d.booking_id
WHERE b.booking_status = 'CONFIRMED';
