package com.booking.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShowResponse {

    private Long showId;
    private String movieTitle;
    private String language;
    private String genre;
    private Integer durationMinutes;
    private String theatreName;
    private String cityName;
    private String screenName;
    private LocalDate showDate;
    private LocalTime showTime;
    private BigDecimal basePrice;
    private Integer availableSeats;
    private String status;
    private boolean afternoonDiscountApplicable;
}
