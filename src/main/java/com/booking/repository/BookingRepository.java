package com.booking.repository;

import com.booking.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    Optional<Booking> findByBookingReference(String bookingReference);

    List<Booking> findByCustomerEmail(String customerEmail);

    @Query("SELECT b FROM Booking b WHERE b.show.id = :showId AND b.status = 'CONFIRMED'")
    List<Booking> findConfirmedBookingsByShow(@Param("showId") Long showId);

    @Query("SELECT SUM(b.numberOfSeats) FROM Booking b WHERE b.show.id = :showId AND b.status = 'CONFIRMED'")
    Integer getTotalBookedSeats(@Param("showId") Long showId);
}
