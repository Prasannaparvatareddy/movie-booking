package com.booking.service;

import com.booking.entity.Show;

import java.math.BigDecimal;

/**
 * Strategy Pattern: Defines contract for discount calculation strategies.
 * Allows easy addition of new discount types without modifying existing code (Open/Closed Principle).
 */
public interface DiscountStrategy {

    /**
     * Calculate discount for a given show and number of seats.
     *
     * @param show        the show being booked
     * @param numSeats    number of seats being booked
     * @param baseAmount  total base amount before discount
     * @return discount amount
     */
    BigDecimal calculateDiscount(Show show, int numSeats, BigDecimal baseAmount);

    /**
     * Returns a human-readable description of the discount.
     */
    String getDiscountDescription(Show show, int numSeats);

    /**
     * Checks if this discount applies to the given show.
     */
    boolean isApplicable(Show show, int numSeats);
}
