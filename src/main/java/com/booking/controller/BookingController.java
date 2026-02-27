package com.booking.controller;

import com.booking.dto.*;
import com.booking.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
@Slf4j
public class BookingController {

    private final BookingService bookingService;

    /**
     * B2C: Book movie tickets.
     * Applies discounts: 20% for afternoon shows (12-5 PM), 50% on 3rd ticket.
     * POST /api/v1/bookings
     *
     * Request body:
     * {
     *   "showId": 2,
     *   "customerName": "John Doe",
     *   "customerEmail": "john@example.com",
     *   "customerPhone": "9876543210",
     *   "seatNumbers": ["A1", "A2", "A3"]
     * }
     */
    @PostMapping
    public ResponseEntity<ApiResponse<BookingResponse>> bookTickets(
            @Valid @RequestBody BookingRequest request) {

        BookingResponse booking = bookingService.bookTickets(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Booking confirmed!", booking));
    }

    /**
     * Bulk booking - Book tickets for multiple shows.
     * POST /api/v1/bookings/bulk
     */
    @PostMapping("/bulk")
    public ResponseEntity<ApiResponse<List<BookingResponse>>> bulkBookTickets(
            @Valid @RequestBody BulkBookingRequest request) {

        List<BookingResponse> bookings = bookingService.bulkBookTickets(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Bulk booking confirmed!", bookings));
    }

    /**
     * Get booking details by reference number.
     * GET /api/v1/bookings/{bookingReference}
     */
    @GetMapping("/{bookingReference}")
    public ResponseEntity<ApiResponse<BookingResponse>> getBooking(
            @PathVariable String bookingReference) {

        BookingResponse booking = bookingService.getBookingByReference(bookingReference);
        return ResponseEntity.ok(ApiResponse.success(booking));
    }

    /**
     * Get all bookings for a customer by email.
     * GET /api/v1/bookings?email=john@example.com
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<BookingResponse>>> getBookingsByCustomer(
            @RequestParam String email) {

        List<BookingResponse> bookings = bookingService.getBookingsByCustomer(email);
        return ResponseEntity.ok(ApiResponse.success(bookings));
    }

    /**
     * Cancel a booking.
     * DELETE /api/v1/bookings/{bookingReference}
     */
    @DeleteMapping("/{bookingReference}")
    public ResponseEntity<ApiResponse<BookingResponse>> cancelBooking(
            @PathVariable String bookingReference) {

        BookingResponse booking = bookingService.cancelBooking(bookingReference);
        return ResponseEntity.ok(ApiResponse.success("Booking cancelled successfully", booking));
    }

    /**
     * Bulk cancellation of multiple bookings.
     * DELETE /api/v1/bookings/bulk
     */
    @DeleteMapping("/bulk")
    public ResponseEntity<ApiResponse<List<BookingResponse>>> bulkCancelBookings(
            @RequestBody List<String> bookingReferences) {

        List<BookingResponse> bookings = bookingService.bulkCancelBookings(bookingReferences);
        return ResponseEntity.ok(ApiResponse.success("Bulk cancellation done", bookings));
    }
}
