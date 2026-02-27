package com.booking.controller;

import com.booking.dto.ApiResponse;
import com.booking.dto.ShowRequest;
import com.booking.dto.ShowResponse;
import com.booking.service.ShowService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
public class ShowController {

    private final ShowService showService;

    /**
     * READ Scenario: Browse theatres currently running a movie in a city on a specific date.
     * GET /api/v1/shows?movie=Avengers&city=Mumbai&date=2025-07-01
     */
    @GetMapping("/shows")
    public ResponseEntity<ApiResponse<List<ShowResponse>>> browseShows(
            @RequestParam String movie,
            @RequestParam String city,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        List<ShowResponse> shows = showService.browseShows(movie, city, date);
        return ResponseEntity.ok(ApiResponse.success(
                "Found " + shows.size() + " shows", shows));
    }

    /**
     * Get shows for a theatre on a specific date.
     * GET /api/v1/theatres/{theatreId}/shows?date=2025-07-01
     */
    @GetMapping("/theatres/{theatreId}/shows")
    public ResponseEntity<ApiResponse<List<ShowResponse>>> getShowsByTheatre(
            @PathVariable Long theatreId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        List<ShowResponse> shows = showService.getShowsByTheatreAndDate(theatreId, date);
        return ResponseEntity.ok(ApiResponse.success(shows));
    }

    /**
     * Get show details by ID.
     * GET /api/v1/shows/{showId}
     */
    @GetMapping("/shows/{showId}")
    public ResponseEntity<ApiResponse<ShowResponse>> getShowById(@PathVariable Long showId) {
        ShowResponse show = showService.getShowById(showId);
        return ResponseEntity.ok(ApiResponse.success(show));
    }

    /**
     * B2B: Theatre creates a new show.
     * POST /api/v1/theatres/{theatreId}/shows
     */
    @PostMapping("/theatres/{theatreId}/shows")
    public ResponseEntity<ApiResponse<ShowResponse>> createShow(
            @PathVariable Long theatreId,
            @Valid @RequestBody ShowRequest request) {

        ShowResponse show = showService.createShow(theatreId, request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Show created successfully", show));
    }

    /**
     * B2B: Theatre updates an existing show.
     * PUT /api/v1/theatres/{theatreId}/shows/{showId}
     */
    @PutMapping("/theatres/{theatreId}/shows/{showId}")
    public ResponseEntity<ApiResponse<ShowResponse>> updateShow(
            @PathVariable Long theatreId,
            @PathVariable Long showId,
            @Valid @RequestBody ShowRequest request) {

        ShowResponse show = showService.updateShow(theatreId, showId, request);
        return ResponseEntity.ok(ApiResponse.success("Show updated successfully", show));
    }

    /**
     * B2B: Theatre deletes/cancels a show.
     * DELETE /api/v1/theatres/{theatreId}/shows/{showId}
     */
    @DeleteMapping("/theatres/{theatreId}/shows/{showId}")
    public ResponseEntity<ApiResponse<Void>> deleteShow(
            @PathVariable Long theatreId,
            @PathVariable Long showId) {

        showService.deleteShow(theatreId, showId);
        return ResponseEntity.ok(ApiResponse.success("Show cancelled successfully", null));
    }

    /**
     * B2B: Theatre updates seat inventory for a show.
     * PATCH /api/v1/theatres/{theatreId}/shows/{showId}/inventory?additionalSeats=20
     */
    @PatchMapping("/theatres/{theatreId}/shows/{showId}/inventory")
    public ResponseEntity<ApiResponse<ShowResponse>> updateSeatInventory(
            @PathVariable Long theatreId,
            @PathVariable Long showId,
            @RequestParam Integer additionalSeats) {

        ShowResponse show = showService.updateSeatInventory(theatreId, showId, additionalSeats);
        return ResponseEntity.ok(ApiResponse.success("Inventory updated successfully", show));
    }
}
