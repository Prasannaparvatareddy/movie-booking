package com.booking.repository;

import com.booking.entity.Show;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ShowRepository extends JpaRepository<Show, Long> {

    @Query("SELECT s FROM Show s JOIN s.screen sc JOIN sc.theatre t JOIN s.movie m " +
           "WHERE t.id = :theatreId AND s.showDate = :showDate AND s.status = 'ACTIVE'")
    List<Show> findByTheatreAndDate(@Param("theatreId") Long theatreId,
                                    @Param("showDate") LocalDate showDate);

    @Query("SELECT s FROM Show s JOIN s.screen sc JOIN sc.theatre t JOIN s.movie m JOIN t.city c " +
           "WHERE LOWER(m.title) = LOWER(:movieTitle) AND s.showDate = :showDate " +
           "AND LOWER(c.name) = LOWER(:cityName) AND s.status = 'ACTIVE' ORDER BY t.name, s.showTime")
    List<Show> findByMovieCityAndDate(@Param("movieTitle") String movieTitle,
                                      @Param("cityName") String cityName,
                                      @Param("showDate") LocalDate showDate);

    @Query("SELECT s FROM Show s JOIN s.screen sc JOIN sc.theatre t " +
           "WHERE t.id = :theatreId AND s.movie.id = :movieId AND s.showDate = :showDate AND s.status = 'ACTIVE'")
    List<Show> findByTheatreMovieAndDate(@Param("theatreId") Long theatreId,
                                         @Param("movieId") Long movieId,
                                         @Param("showDate") LocalDate showDate);

    @Query("SELECT s FROM Show s JOIN s.screen sc WHERE sc.theatre.id = :theatreId")
    List<Show> findAllByTheatreId(@Param("theatreId") Long theatreId);
}
