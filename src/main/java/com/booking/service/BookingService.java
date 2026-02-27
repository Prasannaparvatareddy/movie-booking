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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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

    /**
     * Book movie tickets for a show (WRITE scenario - B2C).
     * Applies discounts: 20% for afternoon shows, 50% on third ticket.
     */
    @Transactional
    public BookingResponse bookTickets(BookingRequest request) {
        log.info("Booking {} seats for show: {}, customer: {}",
                request.getSeatNumbers().size(), request.getShowId(), request.getCustomerEmail());

        Show show = showRepository.findById(request.getShowId())
                .orElseThrow(() -> new ResourceNotFoundException("Show", request.getShowId()));

        validateShowForBooking(show, request.getSeatNumbers().size());

        // Calculate pricing with applicable discounts
        int numSeats = request.getSeatNumbers().size();
        PricingService.PricingResult pricing = pricingService.calculatePrice(show, numSeats);

        // Create booking
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

        // Create booked seats
        List<BookedSeat> bookedSeats = request.getSeatNumbers().stream()
                .map(seatNumber -> BookedSeat.builder()
                        .booking(booking)
                        .seatNumber(seatNumber)
                        .seatPrice(show.getBasePrice().doubleValue())
                        .build())
                .collect(Collectors.toList());

        booking.setBookedSeats(bookedSeats);

        // Reduce available seats
        show.setAvailableSeats(show.getAvailableSeats() - numSeats);
        if (show.getAvailableSeats() == 0) {
            show.setStatus(Show.ShowStatus.HOUSEFUL);
        }
        showRepository.save(show);

        Booking savedBooking = bookingRepository.save(booking);
        log.info("Booking confirmed with reference: {}", bookingReference);

        return mapToBookingResponse(savedBooking, pricing.discountDescription());
    }

    /**
     * Bulk booking - book tickets for multiple shows in a single request.
     */
    @Transactional
    public List<BookingResponse> bulkBookTickets(BulkBookingRequest bulkRequest) {
        log.info("Processing bulk booking with {} requests", bulkRequest.getBookings().size());
        return bulkRequest.getBookings().stream()
                .map(this::bookTickets)
                .collect(Collectors.toList());
    }

    /**
     * Cancel a specific booking.
     */
    @Transactional
    public BookingResponse cancelBooking(String bookingReference) {
        log.info("Cancelling booking: {}", bookingReference);

        Booking booking = bookingRepository.findByBookingReference(bookingReference)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Booking not found with reference: " + bookingReference));

        if (booking.getStatus() == Booking.BookingStatus.CANCELLED) {
            throw new BookingException("Booking is already cancelled");
        }

        booking.setStatus(Booking.BookingStatus.CANCELLED);

        // Restore available seats
        Show show = booking.getShow();
        show.setAvailableSeats(show.getAvailableSeats() + booking.getNumberOfSeats());
        if (show.getStatus() == Show.ShowStatus.HOUSEFUL) {
            show.setStatus(Show.ShowStatus.ACTIVE);
        }
        showRepository.save(show);
        bookingRepository.save(booking);

        log.info("Booking {} cancelled successfully", bookingReference);
        return mapToBookingResponse(booking, "Booking Cancelled");
    }

    /**
     * Bulk cancellation of multiple bookings.
     */
    @Transactional
    public List<BookingResponse> bulkCancelBookings(List<String> bookingReferences) {
        log.info("Bulk cancelling {} bookings", bookingReferences.size());
        return bookingReferences.stream()
                .map(this::cancelBooking)
                .collect(Collectors.toList());
    }

    /**
     * Get booking details by reference.
     */
    @Transactional(readOnly = true)
    public BookingResponse getBookingByReference(String bookingReference) {
        Booking booking = bookingRepository.findByBookingReference(bookingReference)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Booking not found with reference: " + bookingReference));
        return mapToBookingResponse(booking, "");
    }

    /**
     * Get all bookings by customer email.
     */
    @Transactional(readOnly = true)
    public List<BookingResponse> getBookingsByCustomer(String customerEmail) {
        return bookingRepository.findByCustomerEmail(customerEmail).stream()
                .map(b -> mapToBookingResponse(b, ""))
                .collect(Collectors.toList());
    }

    private void validateShowForBooking(Show show, int requestedSeats) {
        if (show.getStatus() != Show.ShowStatus.ACTIVE) {
            throw new BookingException("Show is not available for booking. Status: " + show.getStatus());
        }
        if (show.getShowDate().isBefore(java.time.LocalDate.now())) {
            throw new BookingException("Cannot book tickets for a past show");
        }
        if (show.getAvailableSeats() < requestedSeats) {
            throw new BookingException("Not enough seats available. Available: " + show.getAvailableSeats()
                    + ", Requested: " + requestedSeats);
        }
    }

    private String generateBookingReference() {
        return "BK" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private BookingResponse mapToBookingResponse(Booking booking, String discountDescription) {
        Show show = booking.getShow();
        List<String> seatNumbers = booking.getBookedSeats() != null
                ? booking.getBookedSeats().stream()
                    .map(BookedSeat::getSeatNumber)
                    .collect(Collectors.toList())
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
