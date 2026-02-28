package com.booking.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * B2B: Booking details visible to a theatre partner.
 * Does NOT expose sensitive financial details beyond what is needed.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TheatreBookingResponse {

    private Long bookingId;
    private String bookingReference;
    private String bookingStatus;

    // Show info
    private Long showId;
    private String movieTitle;
    private String screenName;
    private LocalDate showDate;
    private LocalTime showTime;

    // Customer info (theatre can contact for issues)
    private String customerName;
    private String customerEmail;
    private String customerPhone;

    // Seat details
    private Integer numberOfSeats;
    private List<String> seatNumbers;

    // Revenue contribution of this booking to the theatre
    private BigDecimal finalAmount;
    private BigDecimal discountAmount;

    private LocalDateTime bookedAt;
}
