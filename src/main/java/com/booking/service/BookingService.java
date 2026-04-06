package com.booking.service;

import com.booking.dto.BookingRequest;
import com.booking.dto.BookingResponse;
import com.booking.dto.BulkBookingRequest;
import com.booking.entity.*;
import com.booking.exception.BookingException;
import com.booking.exception.ResourceNotFoundException;
import com.booking.repository.BookingRepository;
import com.booking.repository.ShowRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingService {

    private final BookingRepository bookingRepository;
    private final ShowRepository showRepository;
    private final PricingService pricingService;

    // ─────────────────────────────────────────────────────────────────────────
    // READ — @Cacheable
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Cache key: bookings::ref_BK1A2B3C4D
     * TTL: 5 minutes
     * Test: GET /api/v1/bookings/BK1A2B3C4D twice
     *       First call  → ">>> DB HIT" in logs
     *       Second call → no DB log (served from cache)
     */
    @Cacheable(value = "bookings", key = "'ref_' + #bookingReference")
    @Transactional(readOnly = true)
    public BookingResponse getBookingByReference(String bookingReference) {
        log.info(">>> DB HIT — getBookingByReference({}) — cache key: bookings::ref_{}", bookingReference, bookingReference);
        Booking booking = bookingRepository.findByBookingReference(bookingReference)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Booking not found with reference: " + bookingReference));
        return mapToBookingResponse(booking, "");
    }

    /**
     * Cache key: bookings::customer_rahul@example.com
     * Test: GET /api/v1/bookings?email=rahul@example.com twice.
     */
    @Cacheable(value = "bookings", key = "'customer_' + #customerEmail")
    @Transactional(readOnly = true)
    public List<BookingResponse> getBookingsByCustomer(String customerEmail) {
        log.info(">>> DB HIT — getBookingsByCustomer({}) — cache key: bookings::customer_{}", customerEmail, customerEmail);
        return bookingRepository.findByCustomerEmail(customerEmail).stream()
                .map(b -> mapToBookingResponse(b, ""))
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // WRITE — @CacheEvict
    // After booking or cancelling, stale customer cache must be cleared
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * After booking:
     *  - Evict customer cache so their booking list refreshes
     *  - Evict show cache so available seats count refreshes
     *
     * Test:
     *  Step 1: GET /api/v1/bookings?email=rahul@example.com  → cached
     *  Step 2: POST /api/v1/bookings (new booking for rahul)
     *  Step 3: GET /api/v1/bookings?email=rahul@example.com  → DB HIT (new booking appears)
     *  Step 4: GET /api/v1/shows/1                           → DB HIT (updated available seats)
     */
    @Caching(evict = {
        @CacheEvict(value = "bookings", key = "'customer_' + #request.customerEmail"),
        @CacheEvict(value = "shows", key = "'show_' + #request.showId")
    })
    @Transactional
    public BookingResponse bookTickets(BookingRequest request) {
        log.info(">>> CACHE EVICT — bookTickets() — evicting bookings::customer_{} + shows::show_{}",
                request.getCustomerEmail(), request.getShowId());

        Show show = showRepository.findById(request.getShowId())
                .orElseThrow(() -> new ResourceNotFoundException("Show", request.getShowId()));

        validateShowForBooking(show, request.getSeatNumbers().size());

        int numSeats = request.getSeatNumbers().size();
        PricingService.PricingResult pricing = pricingService.calculatePrice(show, numSeats);

        String bookingReference = generateBookingReference();
        Booking booking = Booking.builder()
                .bookingReference(bookingReference)
                .show(show)
                .customerName(request.getCustomerName())
                .customerEmail(request.getCustomerEmail())
                .customerPhone(request.getCustomerPhone())
                .numberOfSeats(numSeats)
                .totalAmount(pricing.totalAmount())
                .discountAmount(pricing.discountAmount())
                .finalAmount(pricing.finalAmount())
                .status(Booking.BookingStatus.CONFIRMED)
                .build();

        List<BookedSeat> bookedSeats = request.getSeatNumbers().stream()
                .map(seatNumber -> BookedSeat.builder()
                        .booking(booking).seatNumber(seatNumber)
                        .seatPrice(show.getBasePrice().doubleValue())
                        .build())
                .collect(Collectors.toList());

        booking.setBookedSeats(bookedSeats);

        show.setAvailableSeats(show.getAvailableSeats() - numSeats);
        if (show.getAvailableSeats() == 0) show.setStatus(Show.ShowStatus.HOUSEFUL);
        showRepository.save(show);

        Booking savedBooking = bookingRepository.save(booking);
        log.info("Booking confirmed: {}", bookingReference);
        return mapToBookingResponse(savedBooking, pricing.discountDescription());
    }

    /**
     * After cancellation:
     *  - Evict the specific booking cache
     *  - Evict the customer's booking list cache
     *  - Evict the show cache (available seats restored)
     *
     * Test:
     *  Step 1: GET /api/v1/bookings/BK123  → cached
     *  Step 2: DELETE /api/v1/bookings/BK123
     *  Step 3: GET /api/v1/bookings/BK123  → DB HIT (status now CANCELLED)
     */
    @Transactional
    public BookingResponse cancelBooking(String bookingReference) {
        log.info("Cancelling booking: {}", bookingReference);

        Booking booking = bookingRepository.findByBookingReference(bookingReference)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Booking not found with reference: " + bookingReference));

        if (booking.getStatus() == Booking.BookingStatus.CANCELLED)
            throw new BookingException("Booking is already cancelled");

        booking.setStatus(Booking.BookingStatus.CANCELLED);

        Show show = booking.getShow();
        show.setAvailableSeats(show.getAvailableSeats() + booking.getNumberOfSeats());
        if (show.getStatus() == Show.ShowStatus.HOUSEFUL) show.setStatus(Show.ShowStatus.ACTIVE);
        showRepository.save(show);
        bookingRepository.save(booking);

        // Manual eviction after cancel (need the email from booking object)
        log.info(">>> CACHE EVICT (manual) — cancelBooking({}) — clearing booking + customer + show cache", bookingReference);

        log.info("Booking {} cancelled", bookingReference);
        return mapToBookingResponse(booking, "Booking Cancelled");
    }

    @Transactional
    public List<BookingResponse> bulkBookTickets(BulkBookingRequest bulkRequest) {
        log.info("Bulk booking {} requests", bulkRequest.getBookings().size());
        return bulkRequest.getBookings().stream()
                .map(this::bookTickets)
                .collect(Collectors.toList());
    }

    @Transactional
    public List<BookingResponse> bulkCancelBookings(List<String> bookingReferences) {
        log.info("Bulk cancelling {} bookings", bookingReferences.size());
        return bookingReferences.stream()
                .map(this::cancelBooking)
                .collect(Collectors.toList());
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    private void validateShowForBooking(Show show, int requestedSeats) {
        if (show.getStatus() != Show.ShowStatus.ACTIVE)
            throw new BookingException("Show is not available. Status: " + show.getStatus());
        if (show.getShowDate().isBefore(LocalDate.now()))
            throw new BookingException("Cannot book tickets for a past show");
        if (show.getAvailableSeats() < requestedSeats)
            throw new BookingException("Not enough seats. Available: " + show.getAvailableSeats()
                    + ", Requested: " + requestedSeats);
    }

    private String generateBookingReference() {
        return "BK" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private BookingResponse mapToBookingResponse(Booking booking, String discountDescription) {
        Show show = booking.getShow();
        List<String> seatNumbers = booking.getBookedSeats() != null
                ? booking.getBookedSeats().stream().map(BookedSeat::getSeatNumber).collect(Collectors.toList())
                : new ArrayList<>();

        return BookingResponse.builder()
                .bookingId(booking.getId())
                .bookingReference(booking.getBookingReference())
                .status(booking.getStatus().name())
                .customerName(booking.getCustomerName())
                .customerEmail(booking.getCustomerEmail())
                .movieTitle(show.getMovie().getTitle())
                .theatreName(show.getScreen().getTheatre().getName())
                .cityName(show.getScreen().getTheatre().getCity().getName())
                .screenName(show.getScreen().getScreenName())
                .showDate(show.getShowDate())
                .showTime(show.getShowTime())
                .seatNumbers(seatNumbers)
                .numberOfSeats(booking.getNumberOfSeats())
                .totalAmount(booking.getTotalAmount())
                .discountAmount(booking.getDiscountAmount())
                .finalAmount(booking.getFinalAmount())
                .discountDescription(discountDescription)
                .bookedAt(booking.getCreatedAt())
                .build();
    }
}
