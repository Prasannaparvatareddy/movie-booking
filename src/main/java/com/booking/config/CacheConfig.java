package com.booking.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Cache Configuration — Caffeine in-memory cache.
 *
 * 4 Caches configured:
 *
 *  "movies"   → 10 min TTL, 200 entries  → getAllMovies, getMovieById, getMoviesByCity
 *  "shows"    →  2 min TTL, 500 entries  → browseShows, getShowById, getShowsByTheatreAndDate
 *  "bookings" →  5 min TTL, 1000 entries → getBookingByReference, getBookingsByCustomer
 *  "theatres" →  5 min TTL, 200 entries  → getTheatreDashboard, getAllBookingsForTheatre
 *
 * How to verify cache is working:
 *  1. Enable TRACE logging: logging.level.org.springframework.cache=TRACE
 *  2. Call any GET endpoint twice
 *  3. First call logs:  ">>> DB HIT — methodName()"
 *  4. Second call logs: nothing (served silently from cache)
 *  5. Or call GET /api/v1/cache/stats to see hit/miss counts live
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager();

        // Movies — rarely change, long TTL
        manager.registerCustomCache("movies",
            Caffeine.newBuilder()
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .maximumSize(200)
                .recordStats()
                .build());

        // Shows — seat counts change on booking, short TTL
        manager.registerCustomCache("shows",
            Caffeine.newBuilder()
                .expireAfterWrite(2, TimeUnit.MINUTES)
                .maximumSize(500)
                .recordStats()
                .build());

        // Bookings — customer lookup, reference lookup
        manager.registerCustomCache("bookings",
            Caffeine.newBuilder()
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .maximumSize(1000)
                .recordStats()
                .build());

        // Theatres — dashboard + booking lists
        manager.registerCustomCache("theatres",
            Caffeine.newBuilder()
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .maximumSize(200)
                .recordStats()
                .build());

        return manager;
    }
}
