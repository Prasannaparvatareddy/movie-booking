package com.booking.service;

import com.booking.dto.TheatreBookingResponse;
import com.booking.dto.TheatreDashboardResponse;
import com.booking.entity.Booking;
import com.booking.entity.BookedSeat;
import com.booking.entity.Theatre;
import com.booking.exception.ResourceNotFoundException;
import com.booking.repository.BookingRepository;
import com.booking.repository.TheatreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TheatreService {

    private final TheatreRepository theatreRepository;
    private final BookingRepository bookingRepository;

    // ─────────────────────────────────────────────────────────────────────────
    // READ — @Cacheable
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Cache key: theatres::dashboard_1
     * TTL: 5 minutes (configured in CacheConfig)
     * Test: GET /api/v1/theatres/1/dashboard twice
     *       First call  → ">>> DB HIT" in logs
     *       Second call → no DB log (served from cache)
     */
    @Cacheable(value = "theatres", key = "'dashboard_' + #theatreId")
    @Transactional(readOnly = true)
    public TheatreDashboardResponse getTheatreDashboard(Long theatreId) {
        log.info(">>> DB HIT — getTheatreDashboard({}) — cache key: theatres::dashboard_{}", theatreId, theatreId);

        Theatre theatre = theatreRepository.findById(theatreId)
                .orElseThrow(() -> new ResourceNotFoundException("Theatre", theatreId));

        BigDecimal totalRevenue = bookingRepository.getTotalRevenueByTheatre(theatreId);
        Integer totalSeatsSold = bookingRepository.getTotalSeatsSoldByTheatre(theatreId);

        List<Object[]> rawSummary = bookingRepository.getShowRevenueSummaryByTheatre(theatreId);
        List<TheatreDashboardResponse.ShowRevenueSummary> showSummaries = rawSummary.stream()
                .map(row -> TheatreDashboardResponse.ShowRevenueSummary.builder()
                        .showId(((Number) row[0]).longValue())
                        .showDate(row[1].toString())
                        .showTime(row[2].toString())
                        .movieTitle((String) row[3])
                        .bookingCount(((Number) row[4]).longValue())
                        .revenue((BigDecimal) row[5])
                        .seatsSold(((Number) row[6]).intValue())
                        .build())
                .collect(Collectors.toList());

        int totalBookings = showSummaries.stream().mapToInt(s -> s.getBookingCount().intValue()).sum();

        return TheatreDashboardResponse.builder()
                .theatreId(theatre.getId())
                .theatreName(theatre.getName())
                .cityName(theatre.getCity().getName())
                .totalRevenue(totalRevenue)
                .totalSeatsSold(totalSeatsSold)
                .totalBookings(totalBookings)
                .showSummaries(showSummaries)
                .build();
    }

    /**
     * Cache key: theatres::bookings_1
     * Test: GET /api/v1/theatres/1/bookings twice.
     */
    @Cacheable(value = "theatres", key = "'bookings_' + #theatreId")
    @Transactional(readOnly = true)
    public List<TheatreBookingResponse> getAllBookingsForTheatre(Long theatreId) {
        log.info(">>> DB HIT — getAllBookingsForTheatre({}) — cache key: theatres::bookings_{}", theatreId, theatreId);
        validateTheatreExists(theatreId);
        return bookingRepository.findAllBookingsByTheatre(theatreId)
                .stream().map(this::mapToTheatreBookingResponse).collect(Collectors.toList());
    }

    /**
     * Cache key: theatres::show_bookings_1_2
     * Test: GET /api/v1/theatres/1/shows/2/bookings twice.
     */
    @Cacheable(value = "theatres", key = "'show_bookings_' + #theatreId + '_' + #showId")
    @Transactional(readOnly = true)
    public List<TheatreBookingResponse> getBookingsForShow(Long theatreId, Long showId) {
        log.info(">>> DB HIT — getBookingsForShow({}, {}) — cache key: theatres::show_bookings_{}_{}", theatreId, showId, theatreId, showId);
        validateTheatreExists(theatreId);
        return bookingRepository.findBookingsByTheatreAndShow(theatreId, showId)
                .stream().map(this::mapToTheatreBookingResponse).collect(Collectors.toList());
    }

    /**
     * Cache key: theatres::date_bookings_1_2025-07-01
     * Test: GET /api/v1/theatres/1/bookings?date=2025-07-01 twice.
     */
    @Cacheable(value = "theatres", key = "'date_bookings_' + #theatreId + '_' + #date")
    @Transactional(readOnly = true)
    public List<TheatreBookingResponse> getBookingsByDate(Long theatreId, LocalDate date) {
        log.info(">>> DB HIT — getBookingsByDate({}, {}) — cache key: theatres::date_bookings_{}_{}", theatreId, date, theatreId, date);
        validateTheatreExists(theatreId);
        return bookingRepository.findBookingsByTheatreAndDate(theatreId, date)
                .stream().map(this::mapToTheatreBookingResponse).collect(Collectors.toList());
    }

    /**
     * Cache key: theatres::revenue_1_2025-07-01
     * Test: GET /api/v1/theatres/1/revenue?date=2025-07-01 twice.
     */
    @Cacheable(value = "theatres", key = "'revenue_' + #theatreId + '_' + #date")
    @Transactional(readOnly = true)
    public BigDecimal getRevenueByDate(Long theatreId, LocalDate date) {
        log.info(">>> DB HIT — getRevenueByDate({}, {}) — cache key: theatres::revenue_{}_{}", theatreId, date, theatreId, date);
        validateTheatreExists(theatreId);
        return bookingRepository.getRevenueByTheatreAndDate(theatreId, date);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Evict theatre cache when bookings change (called from BookingService)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Called after a new booking or cancellation to refresh theatre data.
     */
    @CacheEvict(value = "theatres", allEntries = true)
    public void evictTheatreCache() {
        log.info(">>> CACHE EVICT — evictTheatreCache() — clearing ALL theatres cache entries");
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    private void validateTheatreExists(Long theatreId) {
        if (!theatreRepository.existsById(theatreId))
            throw new ResourceNotFoundException("Theatre", theatreId);
    }

    private TheatreBookingResponse mapToTheatreBookingResponse(Booking booking) {
        List<String> seatNumbers = booking.getBookedSeats() != null
                ? booking.getBookedSeats().stream().map(BookedSeat::getSeatNumber).collect(Collectors.toList())
                : List.of();

        return TheatreBookingResponse.builder()
                .bookingId(booking.getId())
                .bookingReference(booking.getBookingReference())
                .bookingStatus(booking.getStatus().name())
                .showId(booking.getShow().getId())
                .movieTitle(booking.getShow().getMovie().getTitle())
                .screenName(booking.getShow().getScreen().getScreenName())
                .showDate(booking.getShow().getShowDate())
                .showTime(booking.getShow().getShowTime())
                .customerName(booking.getCustomerName())
                .customerEmail(booking.getCustomerEmail())
                .customerPhone(booking.getCustomerPhone())
                .numberOfSeats(booking.getNumberOfSeats())
                .seatNumbers(seatNumbers)
                .finalAmount(booking.getFinalAmount())
                .discountAmount(booking.getDiscountAmount())
                .bookedAt(booking.getCreatedAt())
                .build();
    }
}
