package com.booking.repository;

import com.booking.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    Optional<Booking> findByBookingReference(String bookingReference);

    List<Booking> findByCustomerEmail(String customerEmail);

    // ── B2C ─────────────────────────────────────────────────────────────────

    @Query("SELECT b FROM Booking b WHERE b.show.id = :showId AND b.status = 'CONFIRMED'")
    List<Booking> findConfirmedBookingsByShow(@Param("showId") Long showId);

    @Query("SELECT SUM(b.numberOfSeats) FROM Booking b WHERE b.show.id = :showId AND b.status = 'CONFIRMED'")
    Integer getTotalBookedSeats(@Param("showId") Long showId);

    // ── B2B: Theatre-level booking visibility ────────────────────────────────

    /**
     * All bookings for every show in a specific theatre.
     */
    @Query("SELECT b FROM Booking b " +
           "JOIN b.show s JOIN s.screen sc JOIN sc.theatre t " +
           "WHERE t.id = :theatreId " +
           "ORDER BY s.showDate DESC, s.showTime ASC")
    List<Booking> findAllBookingsByTheatre(@Param("theatreId") Long theatreId);

    /**
     * All bookings for a specific show (theatre can view who booked).
     */
    @Query("SELECT b FROM Booking b " +
           "JOIN b.show s JOIN s.screen sc JOIN sc.theatre t " +
           "WHERE t.id = :theatreId AND s.id = :showId " +
           "ORDER BY b.createdAt DESC")
    List<Booking> findBookingsByTheatreAndShow(@Param("theatreId") Long theatreId,
                                               @Param("showId") Long showId);

    /**
     * All bookings for a theatre on a specific date.
     */
    @Query("SELECT b FROM Booking b " +
           "JOIN b.show s JOIN s.screen sc JOIN sc.theatre t " +
           "WHERE t.id = :theatreId AND s.showDate = :date " +
           "ORDER BY s.showTime ASC, b.createdAt DESC")
    List<Booking> findBookingsByTheatreAndDate(@Param("theatreId") Long theatreId,
                                               @Param("date") LocalDate date);

    /**
     * Total revenue (sum of finalAmount) for a theatre.
     */
    @Query("SELECT COALESCE(SUM(b.finalAmount), 0) FROM Booking b " +
           "JOIN b.show s JOIN s.screen sc JOIN sc.theatre t " +
           "WHERE t.id = :theatreId AND b.status = 'CONFIRMED'")
    java.math.BigDecimal getTotalRevenueByTheatre(@Param("theatreId") Long theatreId);

    /**
     * Revenue for a theatre on a specific date.
     */
    @Query("SELECT COALESCE(SUM(b.finalAmount), 0) FROM Booking b " +
           "JOIN b.show s JOIN s.screen sc JOIN sc.theatre t " +
           "WHERE t.id = :theatreId AND s.showDate = :date AND b.status = 'CONFIRMED'")
    java.math.BigDecimal getRevenueByTheatreAndDate(@Param("theatreId") Long theatreId,
                                                    @Param("date") LocalDate date);

    /**
     * Total seats sold for a theatre.
     */
    @Query("SELECT COALESCE(SUM(b.numberOfSeats), 0) FROM Booking b " +
           "JOIN b.show s JOIN s.screen sc JOIN sc.theatre t " +
           "WHERE t.id = :theatreId AND b.status = 'CONFIRMED'")
    Integer getTotalSeatsSoldByTheatre(@Param("theatreId") Long theatreId);

    /**
     * Booking count and revenue breakdown per show for a theatre (for dashboard).
     */
    @Query("SELECT s.id, s.showDate, s.showTime, m.title, " +
           "COUNT(b.id), COALESCE(SUM(b.finalAmount), 0), COALESCE(SUM(b.numberOfSeats), 0) " +
           "FROM Show s LEFT JOIN s.bookings b " +
           "JOIN s.movie m JOIN s.screen sc JOIN sc.theatre t " +
           "WHERE t.id = :theatreId AND b.status = 'CONFIRMED' " +
           "GROUP BY s.id, s.showDate, s.showTime, m.title " +
           "ORDER BY s.showDate DESC")
    List<Object[]> getShowRevenueSummaryByTheatre(@Param("theatreId") Long theatreId);
}
