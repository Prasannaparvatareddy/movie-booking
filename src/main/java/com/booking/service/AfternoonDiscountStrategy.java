package com.booking.service;

import com.booking.entity.Show;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalTime;

/**
 * Applies 20% discount for tickets booked for afternoon shows (12:00 PM - 5:00 PM).
 */
@Component
public class AfternoonDiscountStrategy implements DiscountStrategy {

    private static final LocalTime AFTERNOON_START = LocalTime.of(12, 0);
    private static final LocalTime AFTERNOON_END = LocalTime.of(17, 0);
    private static final BigDecimal DISCOUNT_RATE = new BigDecimal("0.20");

    @Override
    public BigDecimal calculateDiscount(Show show, int numSeats, BigDecimal baseAmount) {
        if (!isApplicable(show, numSeats)) {
            return BigDecimal.ZERO;
        }
        return baseAmount.multiply(DISCOUNT_RATE).setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public String getDiscountDescription(Show show, int numSeats) {
        if (isApplicable(show, numSeats)) {
            return "20% Afternoon Show Discount applied";
        }
        return "";
    }

    @Override
    public boolean isApplicable(Show show, int numSeats) {
        LocalTime showTime = show.getShowTime();
        return !showTime.isBefore(AFTERNOON_START) && showTime.isBefore(AFTERNOON_END);
    }
}
