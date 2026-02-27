package com.booking.controller;

import com.booking.dto.ApiResponse;
import com.booking.entity.Movie;
import com.booking.service.MovieService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/movies")
@RequiredArgsConstructor
public class MovieController {

    private final MovieService movieService;

    /**
     * Get all movies.
     * GET /api/v1/movies
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<Movie>>> getAllMovies() {
        return ResponseEntity.ok(ApiResponse.success(movieService.getAllMovies()));
    }

    /**
     * Get movie by ID.
     * GET /api/v1/movies/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Movie>> getMovieById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(movieService.getMovieById(id)));
    }

    /**
     * Get movies by city (movies currently running in a city).
     * GET /api/v1/movies?city=Mumbai
     */
    @GetMapping(params = "city")
    public ResponseEntity<ApiResponse<List<Movie>>> getMoviesByCity(@RequestParam String city) {
        return ResponseEntity.ok(ApiResponse.success(movieService.getMoviesByCity(city)));
    }

    /**
     * Get movies by city and language.
     * GET /api/v1/movies?city=Mumbai&language=Hindi
     */
    @GetMapping(params = {"city", "language"})
    public ResponseEntity<ApiResponse<List<Movie>>> getMoviesByCityAndLanguage(
            @RequestParam String city,
            @RequestParam String language) {
        return ResponseEntity.ok(ApiResponse.success(
                movieService.getMoviesByCityAndLanguage(city, language)));
    }

    /**
     * Get movies by genre.
     * GET /api/v1/movies?genre=ACTION
     */
    @GetMapping(params = "genre")
    public ResponseEntity<ApiResponse<List<Movie>>> getMoviesByGenre(@RequestParam String genre) {
        return ResponseEntity.ok(ApiResponse.success(movieService.getMoviesByGenre(genre)));
    }
}
