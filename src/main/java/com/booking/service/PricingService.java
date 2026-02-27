package com.booking.service;

import com.booking.entity.Show;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * PricingService applies all applicable discount strategies and returns the best (maximum) discount.
 * Uses the Strategy Pattern for extensible discount handling.
 */
@Service
public class PricingService {

    private final List<DiscountStrategy> discountStrategies;

    public PricingService(AfternoonDiscountStrategy afternoonDiscountStrategy,
                          ThirdTicketDiscountStrategy thirdTicketDiscountStrategy) {
        this.discountStrategies = List.of(afternoonDiscountStrategy, thirdTicketDiscountStrategy);
    }

    /**
     * Calculate the total amount, discount, and final payable amount.
     */
    public PricingResult calculatePrice(Show show, int numSeats) {
        BigDecimal basePrice = show.getBasePrice();
        BigDecimal totalAmount = basePrice.multiply(BigDecimal.valueOf(numSeats));

        // Apply all applicable discounts (we take the maximum single discount - not stacked)
        BigDecimal maxDiscount = BigDecimal.ZERO;
        String discountDescription = "No discount applied";

        for (DiscountStrategy strategy : discountStrategies) {
            if (strategy.isApplicable(show, numSeats)) {
                BigDecimal discount = strategy.calculateDiscount(show, numSeats, totalAmount);
                if (discount.compareTo(maxDiscount) > 0) {
                    maxDiscount = discount;
                    discountDescription = strategy.getDiscountDescription(show, numSeats);
                }
            }
        }

        BigDecimal finalAmount = totalAmount.subtract(maxDiscount);

        return new PricingResult(totalAmount, maxDiscount, finalAmount, discountDescription);
    }

    /**
     * Check if afternoon discount is applicable for display purposes.
     */
    public boolean isAfternoonShow(Show show) {
        return discountStrategies.stream()
                .filter(s -> s instanceof AfternoonDiscountStrategy)
                .anyMatch(s -> s.isApplicable(show, 1));
    }

    public record PricingResult(
            BigDecimal totalAmount,
            BigDecimal discountAmount,
            BigDecimal finalAmount,
            String discountDescription
    ) {}
}
