package com.booking.service;

import com.booking.entity.Movie;
import com.booking.exception.ResourceNotFoundException;
import com.booking.repository.MovieRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MovieService {

    private final MovieRepository movieRepository;

    // ─────────────────────────────────────────────────────────────────────────
    // READ methods — annotated with @Cacheable
    // First call  → Cache MISS → hits DB → stores in cache → logs "DB HIT"
    // Second call → Cache HIT  → returns from memory → logs "CACHE HIT"
    // Check logs in console to confirm cache is working
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Cache key: movies::all
     * TTL: 10 minutes (configured in CacheConfig)
     * Test: Call GET /api/v1/movies twice — second call returns instantly from cache.
     */
    @Cacheable(value = "movies", key = "'all'")
    @Transactional(readOnly = true)
    public List<Movie> getAllMovies() {
        log.info(">>> DB HIT — getAllMovies() — cache key: movies::all");
        return movieRepository.findAll();
    }

    /**
     * Cache key: movies::1, movies::2, movies::3 (one entry per movie ID)
     * Test: Call GET /api/v1/movies/1 twice — watch logs for DB HIT vs CACHE HIT.
     */
    @Cacheable(value = "movies", key = "#id")
    @Transactional(readOnly = true)
    public Movie getMovieById(Long id) {
        log.info(">>> DB HIT — getMovieById({}) — cache key: movies::{}", id, id);
        return movieRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Movie", id));
    }

    /**
     * Cache key: movies::city_Mumbai, movies::city_Delhi
     * Test: Call GET /api/v1/movies?city=Mumbai twice.
     */
    @Cacheable(value = "movies", key = "'city_' + #cityName")
    @Transactional(readOnly = true)
    public List<Movie> getMoviesByCity(String cityName) {
        log.info(">>> DB HIT — getMoviesByCity({}) — cache key: movies::city_{}", cityName, cityName);
        return movieRepository.findMoviesByCity(cityName);
    }

    /**
     * Cache key: movies::city_Mumbai_lang_English
     */
    @Cacheable(value = "movies", key = "'city_' + #cityName + '_lang_' + #language")
    @Transactional(readOnly = true)
    public List<Movie> getMoviesByCityAndLanguage(String cityName, String language) {
        log.info(">>> DB HIT — getMoviesByCityAndLanguage({}, {}) — cache key: movies::city_{}_lang_{}", cityName, language, cityName, language);
        return movieRepository.findMoviesByCityAndLanguage(cityName, language);
    }

    /**
     * Cache key: movies::genre_ACTION
     */
    @Cacheable(value = "movies", key = "'genre_' + #genre")
    @Transactional(readOnly = true)
    public List<Movie> getMoviesByGenre(String genre) {
        log.info(">>> DB HIT — getMoviesByGenre({}) — cache key: movies::genre_{}", genre, genre);
        return movieRepository.findByGenre(Movie.Genre.valueOf(genre.toUpperCase()));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // WRITE methods — annotated with @CacheEvict
    // When data changes, stale entries are removed so next read hits DB fresh
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Evicts movies::all so the list refreshes after a new movie is added.
     * Test: GET /api/v1/movies (cached) → POST add movie → GET /api/v1/movies again
     *       → second GET will show DB HIT in logs because cache was evicted.
     */
    @CacheEvict(value = "movies", key = "'all'")
    @Transactional
    public Movie addMovie(Movie movie) {
        log.info(">>> CACHE EVICT — addMovie() — evicting movies::all");
        return movieRepository.save(movie);
    }
}
