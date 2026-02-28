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

    /**
     * B2B: Full dashboard — total revenue, total seats sold, booking count, and per-show breakdown.
     */
    @Transactional(readOnly = true)
    public TheatreDashboardResponse getTheatreDashboard(Long theatreId) {
        log.info("Fetching dashboard for theatreId: {}", theatreId);

        Theatre theatre = theatreRepository.findById(theatreId)
                .orElseThrow(() -> new ResourceNotFoundException("Theatre", theatreId));

        BigDecimal totalRevenue = bookingRepository.getTotalRevenueByTheatre(theatreId);
        Integer totalSeatsSold = bookingRepository.getTotalSeatsSoldByTheatre(theatreId);

        // Per-show revenue summary
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

        int totalBookings = showSummaries.stream()
                .mapToInt(s -> s.getBookingCount().intValue())
                .sum();

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
     * B2B: All bookings across all shows for a theatre.
     */
    @Transactional(readOnly = true)
    public List<TheatreBookingResponse> getAllBookingsForTheatre(Long theatreId) {
        log.info("Fetching all bookings for theatreId: {}", theatreId);
        validateTheatreExists(theatreId);
        return bookingRepository.findAllBookingsByTheatre(theatreId)
                .stream()
                .map(this::mapToTheatreBookingResponse)
                .collect(Collectors.toList());
    }

    /**
     * B2B: All bookings for a specific show in a theatre.
     */
    @Transactional(readOnly = true)
    public List<TheatreBookingResponse> getBookingsForShow(Long theatreId, Long showId) {
        log.info("Fetching bookings for theatreId: {}, showId: {}", theatreId, showId);
        validateTheatreExists(theatreId);
        return bookingRepository.findBookingsByTheatreAndShow(theatreId, showId)
                .stream()
                .map(this::mapToTheatreBookingResponse)
                .collect(Collectors.toList());
    }

    /**
     * B2B: All bookings for a theatre on a specific date (useful for daily planning).
     */
    @Transactional(readOnly = true)
    public List<TheatreBookingResponse> getBookingsByDate(Long theatreId, LocalDate date) {
        log.info("Fetching bookings for theatreId: {} on date: {}", theatreId, date);
        validateTheatreExists(theatreId);
        return bookingRepository.findBookingsByTheatreAndDate(theatreId, date)
                .stream()
                .map(this::mapToTheatreBookingResponse)
                .collect(Collectors.toList());
    }

    /**
     * B2B: Revenue for a theatre on a specific date.
     */
    @Transactional(readOnly = true)
    public BigDecimal getRevenueByDate(Long theatreId, LocalDate date) {
        validateTheatreExists(theatreId);
        return bookingRepository.getRevenueByTheatreAndDate(theatreId, date);
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private void validateTheatreExists(Long theatreId) {
        if (!theatreRepository.existsById(theatreId)) {
            throw new ResourceNotFoundException("Theatre", theatreId);
        }
    }

    private TheatreBookingResponse mapToTheatreBookingResponse(Booking booking) {
        List<String> seatNumbers = booking.getBookedSeats() != null
                ? booking.getBookedSeats().stream()
                    .map(BookedSeat::getSeatNumber)
                    .collect(Collectors.toList())
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
