package com.booking.repository;

import com.booking.entity.Theatre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TheatreRepository extends JpaRepository<Theatre, Long> {

    @Query("SELECT DISTINCT t FROM Theatre t JOIN t.city c WHERE LOWER(c.name) = LOWER(:cityName)")
    List<Theatre> findByCityName(@Param("cityName") String cityName);

    @Query("SELECT DISTINCT t FROM Theatre t JOIN t.screens sc JOIN sc.shows s JOIN s.movie m " +
           "WHERE LOWER(m.title) = LOWER(:movieTitle) AND s.showDate = :showDate AND s.status = 'ACTIVE'")
    List<Theatre> findByMovieTitleAndDate(@Param("movieTitle") String movieTitle,
                                          @Param("showDate") LocalDate showDate);

    @Query("SELECT DISTINCT t FROM Theatre t JOIN t.screens sc JOIN sc.shows s JOIN s.movie m " +
           "JOIN t.city c WHERE LOWER(m.title) = LOWER(:movieTitle) AND s.showDate = :showDate " +
           "AND LOWER(c.name) = LOWER(:cityName) AND s.status = 'ACTIVE'")
    List<Theatre> findByMovieTitleAndDateAndCity(@Param("movieTitle") String movieTitle,
                                                  @Param("showDate") LocalDate showDate,
                                                  @Param("cityName") String cityName);
}
