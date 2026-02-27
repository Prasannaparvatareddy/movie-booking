package com.booking.service;

import com.booking.dto.ShowRequest;
import com.booking.dto.ShowResponse;
import com.booking.entity.*;
import com.booking.exception.BookingException;
import com.booking.exception.ResourceNotFoundException;
import com.booking.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    /**
     * Browse theatres running a specific movie in a city on a given date (READ scenario).
     */
    @Transactional(readOnly = true)
    public List<ShowResponse> browseShows(String movieTitle, String cityName, LocalDate date) {
        log.info("Browsing shows for movie: {}, city: {}, date: {}", movieTitle, cityName, date);
        List<Show> shows = showRepository.findByMovieCityAndDate(movieTitle, cityName, date);
        return shows.stream()
                .map(this::mapToShowResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get all shows for a specific theatre on a given date.
     */
    @Transactional(readOnly = true)
    public List<ShowResponse> getShowsByTheatreAndDate(Long theatreId, LocalDate date) {
        log.info("Getting shows for theatreId: {}, date: {}", theatreId, date);
        List<Show> shows = showRepository.findByTheatreAndDate(theatreId, date);
        return shows.stream()
                .map(this::mapToShowResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get show details by ID.
     */
    @Transactional(readOnly = true)
    public ShowResponse getShowById(Long showId) {
        Show show = showRepository.findById(showId)
                .orElseThrow(() -> new ResourceNotFoundException("Show", showId));
        return mapToShowResponse(show);
    }

    /**
     * Theatre creates a new show (WRITE scenario - B2B).
     */
    @Transactional
    public ShowResponse createShow(Long theatreId, ShowRequest request) {
        log.info("Creating show for theatreId: {}, movieId: {}", theatreId, request.getMovieId());

        Movie movie = movieRepository.findById(request.getMovieId())
                .orElseThrow(() -> new ResourceNotFoundException("Movie", request.getMovieId()));

        Screen screen = screenRepository.findById(request.getScreenId())
                .orElseThrow(() -> new ResourceNotFoundException("Screen", request.getScreenId()));

        // Validate screen belongs to the theatre
        if (!screen.getTheatre().getId().equals(theatreId)) {
            throw new BookingException("Screen does not belong to the specified theatre");
        }

        // Check for conflicting shows on the same screen at the same time
        boolean hasConflict = showRepository
                .findByTheatreMovieAndDate(theatreId, request.getMovieId(), request.getShowDate())
                .stream()
                .anyMatch(s -> s.getShowTime().equals(request.getShowTime())
                        && s.getScreen().getId().equals(request.getScreenId()));

        if (hasConflict) {
            throw new BookingException("A show already exists for this screen at the same time");
        }

        Show show = Show.builder()
                .movie(movie)
                .screen(screen)
                .showDate(request.getShowDate())
                .showTime(request.getShowTime())
                .basePrice(request.getBasePrice())
                .availableSeats(screen.getTotalSeats())
                .status(Show.ShowStatus.ACTIVE)
                .build();

        Show savedShow = showRepository.save(show);
        log.info("Show created with id: {}", savedShow.getId());
        return mapToShowResponse(savedShow);
    }

    /**
     * Theatre updates an existing show (WRITE scenario - B2B).
     */
    @Transactional
    public ShowResponse updateShow(Long theatreId, Long showId, ShowRequest request) {
        log.info("Updating showId: {} for theatreId: {}", showId, theatreId);

        Show show = showRepository.findById(showId)
                .orElseThrow(() -> new ResourceNotFoundException("Show", showId));

        // Validate show belongs to theatre
        if (!show.getScreen().getTheatre().getId().equals(theatreId)) {
            throw new BookingException("Show does not belong to the specified theatre");
        }

        if (show.getStatus() == Show.ShowStatus.CANCELLED) {
            throw new BookingException("Cannot update a cancelled show");
        }

        show.setShowDate(request.getShowDate());
        show.setShowTime(request.getShowTime());
        show.setBasePrice(request.getBasePrice());

        Show updatedShow = showRepository.save(show);
        return mapToShowResponse(updatedShow);
    }

    /**
     * Theatre deletes/cancels a show (WRITE scenario - B2B).
     */
    @Transactional
    public void deleteShow(Long theatreId, Long showId) {
        log.info("Cancelling showId: {} for theatreId: {}", showId, theatreId);

        Show show = showRepository.findById(showId)
                .orElseThrow(() -> new ResourceNotFoundException("Show", showId));

        if (!show.getScreen().getTheatre().getId().equals(theatreId)) {
            throw new BookingException("Show does not belong to the specified theatre");
        }

        show.setStatus(Show.ShowStatus.CANCELLED);
        showRepository.save(show);
        log.info("Show {} cancelled", showId);
    }

    /**
     * Update seat inventory for a show (WRITE scenario - B2B).
     */
    @Transactional
    public ShowResponse updateSeatInventory(Long theatreId, Long showId, Integer additionalSeats) {
        Show show = showRepository.findById(showId)
                .orElseThrow(() -> new ResourceNotFoundException("Show", showId));

        if (!show.getScreen().getTheatre().getId().equals(theatreId)) {
            throw new BookingException("Show does not belong to the specified theatre");
        }

        int newAvailable = show.getAvailableSeats() + additionalSeats;
        if (newAvailable < 0) {
            throw new BookingException("Cannot reduce seats below 0. Current available: " + show.getAvailableSeats());
        }

        show.setAvailableSeats(newAvailable);
        showRepository.save(show);
        return mapToShowResponse(show);
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
