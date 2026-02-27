package com.booking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Movie Ticket Booking Platform - Main Application
 *
 * Architecture Overview:
 * ----------------------
 * B2B: Theatre Partners can onboard and manage shows (create/update/delete shows, manage seat inventory)
 * B2C: End customers can browse movies, check show timings, and book tickets with discounts
 *
 * Design Patterns Used:
 * ---------------------
 * 1. Strategy Pattern  - Discount calculation (AfternoonDiscountStrategy, ThirdTicketDiscountStrategy)
 * 2. Builder Pattern   - Entity and DTO construction via Lombok @Builder
 * 3. Repository Pattern - Data access via Spring Data JPA repositories
 * 4. Service Layer     - Business logic separated from controllers
 * 5. DTO Pattern       - Request/Response objects decoupled from entities
 *
 * Discount Rules:
 * ---------------
 * - 50% discount on the third ticket (every 3rd ticket in a booking)
 * - 20% discount for tickets booked in afternoon shows (12:00 PM - 5:00 PM)
 * - Best applicable discount is applied (not stacked)
 *
 * H2 Console: http://localhost:8080/h2-console
 * JDBC URL:   jdbc:h2:mem:moviebookingdb
 */
@SpringBootApplication
public class MovieBookingApplication {

    public static void main(String[] args) {
        SpringApplication.run(MovieBookingApplication.class, args);
    }
}
