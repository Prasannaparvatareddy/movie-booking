package com.booking.service;

import com.booking.entity.Show;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Applies 50% discount on every third ticket in a booking.
 * Example: 3 tickets at ₹300 each → tickets 1 & 2 at ₹300, ticket 3 at ₹150 → total ₹750 (discount ₹150)
 */
@Component
public class ThirdTicketDiscountStrategy implements DiscountStrategy {

    private static final BigDecimal DISCOUNT_RATE = new BigDecimal("0.50");

    @Override
    public BigDecimal calculateDiscount(Show show, int numSeats, BigDecimal baseAmount) {
        if (!isApplicable(show, numSeats)) {
            return BigDecimal.ZERO;
        }

        // Count number of third tickets (floor division gives us count of complete groups of 3)
        int thirdTicketCount = numSeats / 3;
        BigDecimal pricePerSeat = show.getBasePrice();
        BigDecimal discountPerThirdTicket = pricePerSeat.multiply(DISCOUNT_RATE);

        return discountPerThirdTicket
                .multiply(BigDecimal.valueOf(thirdTicketCount))
                .setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public String getDiscountDescription(Show show, int numSeats) {
        if (isApplicable(show, numSeats)) {
            int thirdTicketCount = numSeats / 3;
            return "50% discount applied on " + thirdTicketCount + " third ticket(s)";
        }
        return "";
    }

    @Override
    public boolean isApplicable(Show show, int numSeats) {
        return numSeats >= 3;
    }
}
