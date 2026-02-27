package com.booking.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingResponse {

    private Long bookingId;
    private String bookingReference;
    private String status;

    // Customer Info
    private String customerName;
    private String customerEmail;

    // Show Info
    private String movieTitle;
    private String theatreName;
    private String cityName;
    private String screenName;
    private LocalDate showDate;
    private LocalTime showTime;

    // Seat Info
    private List<String> seatNumbers;
    private Integer numberOfSeats;

    // Pricing
    private BigDecimal totalAmount;
    private BigDecimal discountAmount;
    private BigDecimal finalAmount;
    private String discountDescription;

    private LocalDateTime bookedAt;
}
