package com.booking.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "booked_seat")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookedSeat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnoreProperties({"bookedSeats", "hibernateLazyInitializer", "handler"})
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @Column(name = "seat_number", nullable = false)
    private String seatNumber;

    @Column(name = "seat_price", nullable = false)
    private Double seatPrice;
}
