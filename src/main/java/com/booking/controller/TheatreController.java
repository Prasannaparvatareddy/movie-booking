package com.booking.controller;

import com.booking.dto.ApiResponse;
import com.booking.dto.TheatreBookingResponse;
import com.booking.dto.TheatreDashboardResponse;
import com.booking.service.TheatreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * B2B Controller — APIs exclusively for theatre partners.
 *
 * A theatre partner can:
 *   1. View a full dashboard (revenue, seats sold, per-show breakdown)
 *   2. See all bookings across their shows
 *   3. Filter bookings by show or by date
 *   4. Check daily revenue
 *
 * Show management (create/update/delete/inventory) is in ShowController.
 */
@RestController
@RequestMapping("/api/v1/theatres/{theatreId}")
@RequiredArgsConstructor
@Slf4j
public class TheatreController {

    private final TheatreService theatreService;

    // ── Dashboard ────────────────────────────────────────────────────────────

    /**
     * B2B: Theatre dashboard — total revenue, seats sold, booking count, and per-show breakdown.
     *
     * GET /api/v1/theatres/{theatreId}/dashboard
     *
     * Sample Response:
     * {
     *   "theatreName": "PVR Juhu",
     *   "totalRevenue": 45000.00,
     *   "totalSeatsSold": 180,
     *   "totalBookings": 62,
     *   "showSummaries": [
     *     { "movieTitle": "Avengers", "showDate": "2025-07-01", "revenue": 15000, "seatsSold": 60 }
     *   ]
     * }
     */
    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<TheatreDashboardResponse>> getDashboard(
            @PathVariable Long theatreId) {

        TheatreDashboardResponse dashboard = theatreService.getTheatreDashboard(theatreId);
        return ResponseEntity.ok(ApiResponse.success("Theatre dashboard", dashboard));
    }

    // ── Booking Visibility ────────────────────────────────────────────────────

    /**
     * B2B: All bookings across every show in this theatre.
     *
     * GET /api/v1/theatres/{theatreId}/bookings
     */
    @GetMapping("/bookings")
    public ResponseEntity<ApiResponse<List<TheatreBookingResponse>>> getAllBookings(
            @PathVariable Long theatreId) {

        List<TheatreBookingResponse> bookings = theatreService.getAllBookingsForTheatre(theatreId);
        return ResponseEntity.ok(ApiResponse.success(
                "Found " + bookings.size() + " bookings", bookings));
    }

    /**
     * B2B: All bookings for a specific show in this theatre.
     * Useful to see who is coming for a particular show (seat allocation, customer list).
     *
     * GET /api/v1/theatres/{theatreId}/shows/{showId}/bookings
     */
    @GetMapping("/shows/{showId}/bookings")
    public ResponseEntity<ApiResponse<List<TheatreBookingResponse>>> getBookingsForShow(
            @PathVariable Long theatreId,
            @PathVariable Long showId) {

        List<TheatreBookingResponse> bookings = theatreService.getBookingsForShow(theatreId, showId);
        return ResponseEntity.ok(ApiResponse.success(
                "Found " + bookings.size() + " bookings for show " + showId, bookings));
    }

    /**
     * B2B: All bookings for a specific date (daily planning / staffing).
     *
     * GET /api/v1/theatres/{theatreId}/bookings?date=2025-07-01
     */
    @GetMapping(value = "/bookings", params = "date")
    public ResponseEntity<ApiResponse<List<TheatreBookingResponse>>> getBookingsByDate(
            @PathVariable Long theatreId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        List<TheatreBookingResponse> bookings = theatreService.getBookingsByDate(theatreId, date);
        return ResponseEntity.ok(ApiResponse.success(
                "Found " + bookings.size() + " bookings on " + date, bookings));
    }

    /**
     * B2B: Revenue for a specific date (daily revenue report).
     *
     * GET /api/v1/theatres/{theatreId}/revenue?date=2025-07-01
     */
    @GetMapping("/revenue")
    public ResponseEntity<ApiResponse<BigDecimal>> getRevenueByDate(
            @PathVariable Long theatreId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        BigDecimal revenue = theatreService.getRevenueByDate(theatreId, date);
        return ResponseEntity.ok(ApiResponse.success(
                "Revenue for " + date, revenue));
    }
}
