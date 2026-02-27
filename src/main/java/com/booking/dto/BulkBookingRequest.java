package com.booking.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BulkBookingRequest {

    @NotEmpty(message = "At least one booking request is required")
    @Valid
    private List<BookingRequest> bookings;
}
