package ru.practicum.main.stats.analyzer.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.practicum.main.stats.analyzer.model.Interaction;

import java.util.List;

@Repository
public interface InteractionRepository extends JpaRepository<Interaction, Interaction.InteractionId> {
    @Query("""
            SELECT i.eventId, SUM(i.rating)
            FROM Interaction i
            WHERE i.eventId IN :eventIds
            GROUP BY i.eventId
            """)
    List<Object[]> sumRatingsByEventIds(@Param("eventIds") List<Long> eventIds);

    @Query("""
        SELECT i.eventId
        FROM Interaction i
        WHERE i.userId = :userId
        """)
    List<Long> findEventIdsByUserId(@Param("userId") long userId);

    @Query("""
        SELECT i
        FROM Interaction i
        WHERE i.userId = :userId
        ORDER BY i.timestamp DESC
        """)
    List<Interaction> findRecentByUserId(@Param("userId") long userId, Pageable pageable);

    @Query("""
        SELECT i.eventId, i.rating
        FROM Interaction i
        WHERE i.userId = :userId
        """)
    List<Object[]> findRatingsByUserId(@Param("userId") long userId);
}
