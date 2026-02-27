package com.booking.service;

import com.booking.entity.Movie;
import com.booking.entity.Show;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

class PricingServiceTest {

    private PricingService pricingService;
    private Show afternoonShow;
    private Show eveningShow;

    @BeforeEach
    void setUp() {
        AfternoonDiscountStrategy afternoonStrategy = new AfternoonDiscountStrategy();
        ThirdTicketDiscountStrategy thirdTicketStrategy = new ThirdTicketDiscountStrategy();
        pricingService = new PricingService(afternoonStrategy, thirdTicketStrategy);

        // Afternoon show at 2 PM
        afternoonShow = new Show();
        afternoonShow.setShowDate(LocalDate.now().plusDays(1));
        afternoonShow.setShowTime(LocalTime.of(14, 0));
        afternoonShow.setBasePrice(new BigDecimal("250.00"));
        afternoonShow.setStatus(Show.ShowStatus.ACTIVE);

        // Evening show at 7 PM
        eveningShow = new Show();
        eveningShow.setShowDate(LocalDate.now().plusDays(1));
        eveningShow.setShowTime(LocalTime.of(19, 0));
        eveningShow.setBasePrice(new BigDecimal("300.00"));
        eveningShow.setStatus(Show.ShowStatus.ACTIVE);
    }

    @Test
    @DisplayName("Afternoon show: 20% discount applied on 2 tickets")
    void testAfternoonDiscountTwoTickets() {
        PricingService.PricingResult result = pricingService.calculatePrice(afternoonShow, 2);

        assertEquals(new BigDecimal("500.00"), result.totalAmount());
        assertEquals(new BigDecimal("100.00"), result.discountAmount()); // 20% of 500
        assertEquals(new BigDecimal("400.00"), result.finalAmount());
        assertTrue(result.discountDescription().contains("Afternoon"));
    }

    @Test
    @DisplayName("3 tickets on afternoon show: best discount wins (50% on 3rd = 125 vs 20% of 750 = 150)")
    void testThreeTicketsAfternoonShow_BestDiscountApplied() {
        // 3 tickets at 250 each = 750 total
        // Afternoon discount: 20% of 750 = 150
        // Third ticket discount: 50% of 250 = 125
        // Best discount (afternoon) = 150 should be applied
        PricingService.PricingResult result = pricingService.calculatePrice(afternoonShow, 3);

        assertEquals(new BigDecimal("750.00"), result.totalAmount());
        assertEquals(new BigDecimal("150.00"), result.discountAmount());
        assertEquals(new BigDecimal("600.00"), result.finalAmount());
    }

    @Test
    @DisplayName("3 tickets on evening show: 50% on 3rd ticket only")
    void testThirdTicketDiscountEveningShow() {
        // 3 tickets at 300 each = 900 total
        // Third ticket discount: 50% of 300 = 150
        PricingService.PricingResult result = pricingService.calculatePrice(eveningShow, 3);

        assertEquals(new BigDecimal("900.00"), result.totalAmount());
        assertEquals(new BigDecimal("150.00"), result.discountAmount()); // 50% of 300
        assertEquals(new BigDecimal("750.00"), result.finalAmount());
        assertTrue(result.discountDescription().contains("50%"));
    }

    @Test
    @DisplayName("2 tickets on evening show: no discount")
    void testNoDiscountEveningTwoTickets() {
        PricingService.PricingResult result = pricingService.calculatePrice(eveningShow, 2);

        assertEquals(new BigDecimal("600.00"), result.totalAmount());
        assertEquals(BigDecimal.ZERO, result.discountAmount());
        assertEquals(new BigDecimal("600.00"), result.finalAmount());
    }

    @Test
    @DisplayName("6 tickets on evening show: 50% on 2 third tickets")
    void testSixTicketsThirdTicketDiscount() {
        // 6 tickets at 300 each = 1800 total
        // 2 third tickets (at positions 3 and 6) each 50% off
        // Discount = 2 * (300 * 50%) = 300
        PricingService.PricingResult result = pricingService.calculatePrice(eveningShow, 6);

        assertEquals(new BigDecimal("1800.00"), result.totalAmount());
        assertEquals(new BigDecimal("300.00"), result.discountAmount());
        assertEquals(new BigDecimal("1500.00"), result.finalAmount());
    }

    @Test
    @DisplayName("Afternoon show detection")
    void testIsAfternoonShow() {
        assertTrue(pricingService.isAfternoonShow(afternoonShow));
        assertFalse(pricingService.isAfternoonShow(eveningShow));
    }
}
