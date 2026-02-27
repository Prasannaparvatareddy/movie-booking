package com.booking.repository;

import com.booking.entity.Movie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MovieRepository extends JpaRepository<Movie, Long> {

    List<Movie> findByLanguageIgnoreCase(String language);

    List<Movie> findByGenre(Movie.Genre genre);

    @Query("SELECT DISTINCT m FROM Movie m JOIN m.shows s JOIN s.screen sc JOIN sc.theatre t JOIN t.city c " +
           "WHERE LOWER(c.name) = LOWER(:cityName) AND s.status = 'ACTIVE'")
    List<Movie> findMoviesByCity(@Param("cityName") String cityName);

    @Query("SELECT DISTINCT m FROM Movie m JOIN m.shows s JOIN s.screen sc JOIN sc.theatre t JOIN t.city c " +
           "WHERE LOWER(c.name) = LOWER(:cityName) AND LOWER(m.language) = LOWER(:language) AND s.status = 'ACTIVE'")
    List<Movie> findMoviesByCityAndLanguage(@Param("cityName") String cityName, @Param("language") String language);
}
