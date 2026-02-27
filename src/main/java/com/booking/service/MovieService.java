package com.booking.service;

import com.booking.entity.Movie;
import com.booking.exception.ResourceNotFoundException;
import com.booking.repository.MovieRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MovieService {

    private final MovieRepository movieRepository;

    @Transactional(readOnly = true)
    public List<Movie> getAllMovies() {
        return movieRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Movie getMovieById(Long id) {
        return movieRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Movie", id));
    }

    @Transactional(readOnly = true)
    public List<Movie> getMoviesByCity(String cityName) {
        log.info("Fetching movies for city: {}", cityName);
        return movieRepository.findMoviesByCity(cityName);
    }

    @Transactional(readOnly = true)
    public List<Movie> getMoviesByCityAndLanguage(String cityName, String language) {
        return movieRepository.findMoviesByCityAndLanguage(cityName, language);
    }

    @Transactional(readOnly = true)
    public List<Movie> getMoviesByGenre(String genre) {
        return movieRepository.findByGenre(Movie.Genre.valueOf(genre.toUpperCase()));
    }

    @Transactional
    public Movie addMovie(Movie movie) {
        return movieRepository.save(movie);
    }
}
