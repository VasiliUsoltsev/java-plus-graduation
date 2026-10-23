package ru.practicum.stat.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.practicum.stat.dto.ViewStats;
import ru.practicum.stat.model.Hit;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface StatsRepository extends JpaRepository<Hit, Long> {

    @Query("SELECT new ru.practicum.stat.dto.ViewStats(eh.app, eh.uri, COUNT(DISTINCT eh.ip)) " +
            "FROM Hit eh " +
            "WHERE eh.timestamp BETWEEN :start AND :end " +
            "AND eh.uri IN :uris " +
            "GROUP BY eh.app, eh.uri " +
            "ORDER BY COUNT(DISTINCT eh.ip) DESC")
    List<ViewStats> getStatsWithUniqueIpAndUris(@Param("start") LocalDateTime start,
                                                   @Param("end") LocalDateTime end,
                                                   @Param("uris") List<String> uris);

    @Query("SELECT new ru.practicum.stat.dto.ViewStats(eh.app, eh.uri, COUNT(DISTINCT eh.ip)) " +
            "FROM Hit eh " +
            "WHERE eh.timestamp BETWEEN :start AND :end " +
            "GROUP BY eh.app, eh.uri " +
            "ORDER BY COUNT(DISTINCT eh.ip) DESC")
    List<ViewStats> getStatsWithUniqueIpWithoutUris(@Param("start") LocalDateTime start,
                                                       @Param("end") LocalDateTime end);

    @Query("SELECT new ru.practicum.stat.dto.ViewStats(eh.app, eh.uri, COUNT(eh.ip)) " +
            "FROM Hit eh " +
            "WHERE eh.timestamp BETWEEN :start AND :end " +
            "AND eh.uri IN :uris " +
            "GROUP BY eh.app, eh.uri " +
            "ORDER BY COUNT(eh.ip) DESC")
    List<ViewStats> getStatsAllAndUris(@Param("start") LocalDateTime start,
                                          @Param("end") LocalDateTime end,
                                          @Param("uris") List<String> uris);

    @Query("SELECT new ru.practicum.stat.dto.ViewStats(eh.app, eh.uri, COUNT(eh.ip)) " +
            "FROM Hit eh " +
            "WHERE eh.timestamp BETWEEN :start AND :end " +
            "GROUP BY eh.app, eh.uri " +
            "ORDER BY COUNT(eh.ip) DESC")
    List<ViewStats> getStatsAllWithoutUris(@Param("start") LocalDateTime start,
                                              @Param("end") LocalDateTime end);
}