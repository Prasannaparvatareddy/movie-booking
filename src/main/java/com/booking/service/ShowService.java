package com.booking.service;

import com.booking.dto.ShowRequest;
import com.booking.dto.ShowResponse;
import com.booking.entity.*;
import com.booking.exception.BookingException;
import com.booking.exception.ResourceNotFoundException;
import com.booking.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShowService {

    private final ShowRepository showRepository;
    private final MovieRepository movieRepository;
    private final ScreenRepository screenRepository;
    private final PricingService pricingService;

    // ─────────────────────────────────────────────────────────────────────────
    // READ — @Cacheable
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Cache key: shows::browse_Avengers: Endgame_Mumbai_2025-07-01
     * TTL: 2 minutes
     * Test: Call GET /api/v1/shows?movie=Avengers: Endgame&city=Mumbai&date=2025-07-01 twice
     *       First call  → ">>> DB HIT" in logs
     *       Second call → no log line (served from cache silently)
     */
    @Cacheable(value = "shows", key = "'browse_' + #movieTitle + '_' + #cityName + '_' + #date")
    @Transactional(readOnly = true)
    public List<ShowResponse> browseShows(String movieTitle, String cityName, LocalDate date) {
        log.info(">>> DB HIT — browseShows({}, {}, {}) — cache key: shows::browse_{}_{}_{}", movieTitle, cityName, date, movieTitle, cityName, date);
        return showRepository.findByMovieCityAndDate(movieTitle, cityName, date)
                .stream().map(this::mapToShowResponse).collect(Collectors.toList());
    }

    /**
     * Cache key: shows::theatre_1_2025-07-01
     * Test: Call GET /api/v1/theatres/1/shows?date=2025-07-01 twice.
     */
    @Cacheable(value = "shows", key = "'theatre_' + #theatreId + '_' + #date")
    @Transactional(readOnly = true)
    public List<ShowResponse> getShowsByTheatreAndDate(Long theatreId, LocalDate date) {
        log.info(">>> DB HIT — getShowsByTheatreAndDate({}, {}) — cache key: shows::theatre_{}_{}", theatreId, date, theatreId, date);
        return showRepository.findByTheatreAndDate(theatreId, date)
                .stream().map(this::mapToShowResponse).collect(Collectors.toList());
    }

    /**
     * Cache key: shows::show_1, shows::show_2
     * Test: Call GET /api/v1/shows/1 twice.
     */
    @Cacheable(value = "shows", key = "'show_' + #showId")
    @Transactional(readOnly = true)
    public ShowResponse getShowById(Long showId) {
        log.info(">>> DB HIT — getShowById({}) — cache key: shows::show_{}", showId, showId);
        Show show = showRepository.findById(showId)
                .orElseThrow(() -> new ResourceNotFoundException("Show", showId));
        return mapToShowResponse(show);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // WRITE — @CacheEvict
    // When shows change, relevant cache entries must be cleared
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Evicts ALL show cache entries because a new show must appear in browse results.
     * Test: GET shows (cached) → POST create show → GET shows again → DB HIT in logs
     */
    @CacheEvict(value = "shows", allEntries = true)
    @Transactional
    public ShowResponse createShow(Long theatreId, ShowRequest request) {
        log.info(">>> CACHE EVICT — createShow() — evicting ALL shows cache entries");

        Movie movie = movieRepository.findById(request.getMovieId())
                .orElseThrow(() -> new ResourceNotFoundException("Movie", request.getMovieId()));
        Screen screen = screenRepository.findById(request.getScreenId())
                .orElseThrow(() -> new ResourceNotFoundException("Screen", request.getScreenId()));

        validateScreenBelongsToTheatre(screen, theatreId);
        validateNoConflictingShow(theatreId, request);

        Show show = Show.builder()
                .movie(movie).screen(screen)
                .showDate(request.getShowDate()).showTime(request.getShowTime())
                .basePrice(request.getBasePrice())
                .availableSeats(screen.getTotalSeats())
                .status(Show.ShowStatus.ACTIVE)
                .build();

        Show saved = showRepository.save(show);
        log.info("Show created with id: {}", saved.getId());
        return mapToShowResponse(saved);
    }

    /**
     * Evicts the specific show entry + all browse cache (show time/price may have changed).
     */
    @Caching(evict = {
        @CacheEvict(value = "shows", key = "'show_' + #showId"),
        @CacheEvict(value = "shows", allEntries = true)
    })
    @Transactional
    public ShowResponse updateShow(Long theatreId, Long showId, ShowRequest request) {
        log.info(">>> CACHE EVICT — updateShow({}) — evicting shows::show_{} + all browse entries", showId, showId);

        Show show = showRepository.findById(showId)
                .orElseThrow(() -> new ResourceNotFoundException("Show", showId));
        validateShowBelongsToTheatre(show, theatreId);
        validateShowNotCancelled(show);

        show.setShowDate(request.getShowDate());
        show.setShowTime(request.getShowTime());
        show.setBasePrice(request.getBasePrice());
        return mapToShowResponse(showRepository.save(show));
    }

    /**
     * Evicts all show cache entries — cancelled show must disappear from browse.
     */
    @CacheEvict(value = "shows", allEntries = true)
    @Transactional
    public void deleteShow(Long theatreId, Long showId) {
        log.info(">>> CACHE EVICT — deleteShow({}) — evicting ALL shows cache entries", showId);

        Show show = showRepository.findById(showId)
                .orElseThrow(() -> new ResourceNotFoundException("Show", showId));
        validateShowBelongsToTheatre(show, theatreId);
        show.setStatus(Show.ShowStatus.CANCELLED);
        showRepository.save(show);
    }

    /**
     * Evicts only the specific show cache — available seats count has changed.
     * Test: GET /api/v1/shows/3 (cached) → PATCH inventory → GET /api/v1/shows/3
     *       → third call hits DB again, shows updated seat count
     */
    @CacheEvict(value = "shows", key = "'show_' + #showId")
    @Transactional
    public ShowResponse updateSeatInventory(Long theatreId, Long showId, Integer additionalSeats) {
        log.info(">>> CACHE EVICT — updateSeatInventory({}) — evicting shows::show_{}", showId, showId);

        Show show = showRepository.findById(showId)
                .orElseThrow(() -> new ResourceNotFoundException("Show", showId));
        validateShowBelongsToTheatre(show, theatreId);

        int newAvailable = show.getAvailableSeats() + additionalSeats;
        if (newAvailable < 0)
            throw new BookingException("Cannot reduce seats below 0. Current: " + show.getAvailableSeats());

        show.setAvailableSeats(newAvailable);
        showRepository.save(show);
        return mapToShowResponse(show);
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    private void validateScreenBelongsToTheatre(Screen screen, Long theatreId) {
        if (!screen.getTheatre().getId().equals(theatreId))
            throw new BookingException("Screen does not belong to the specified theatre");
    }

    private void validateShowBelongsToTheatre(Show show, Long theatreId) {
        if (!show.getScreen().getTheatre().getId().equals(theatreId))
            throw new BookingException("Show does not belong to the specified theatre");
    }

    private void validateShowNotCancelled(Show show) {
        if (show.getStatus() == Show.ShowStatus.CANCELLED)
            throw new BookingException("Cannot update a cancelled show");
    }

    private void validateNoConflictingShow(Long theatreId, ShowRequest request) {
        boolean hasConflict = showRepository
                .findByTheatreMovieAndDate(theatreId, request.getMovieId(), request.getShowDate())
                .stream()
                .anyMatch(s -> s.getShowTime().equals(request.getShowTime())
                        && s.getScreen().getId().equals(request.getScreenId()));
        if (hasConflict)
            throw new BookingException("A show already exists for this screen at the same time");
    }

    private ShowResponse mapToShowResponse(Show show) {
        return ShowResponse.builder()
                .showId(show.getId())
                .movieTitle(show.getMovie().getTitle())
                .language(show.getMovie().getLanguage())
                .genre(show.getMovie().getGenre().name())
                .durationMinutes(show.getMovie().getDurationMinutes())
                .theatreName(show.getScreen().getTheatre().getName())
                .cityName(show.getScreen().getTheatre().getCity().getName())
                .screenName(show.getScreen().getScreenName())
                .showDate(show.getShowDate())
                .showTime(show.getShowTime())
                .basePrice(show.getBasePrice())
                .availableSeats(show.getAvailableSeats())
                .status(show.getStatus().name())
                .afternoonDiscountApplicable(pricingService.isAfternoonShow(show))
                .build();
    }
}
