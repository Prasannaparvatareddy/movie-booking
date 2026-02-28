package com.booking.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * B2B: Summary view for a theatre — total revenue, seats sold, bookings, and per-show breakdown.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TheatreDashboardResponse {

    private Long theatreId;
    private String theatreName;
    private String cityName;

    // Aggregate stats
    private BigDecimal totalRevenue;
    private Integer totalSeatsSold;
    private Integer totalBookings;

    // Per-show breakdown
    private List<ShowRevenueSummary> showSummaries;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ShowRevenueSummary {
        private Long showId;
        private String movieTitle;
        private String showDate;
        private String showTime;
        private Long bookingCount;
        private Integer seatsSold;
        private BigDecimal revenue;
    }
}
