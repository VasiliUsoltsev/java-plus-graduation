package ru.practicum.main.stats.analyzer.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.main.stats.analyzer.model.Interaction;

import java.time.Instant;
import java.util.List;

@Repository
public interface InteractionRepository extends JpaRepository<Interaction, Interaction.InteractionId> {
    @Modifying
    @Transactional
    @Query(value = """
            INSERT INTO interactions (user_id, event_id, rating, ts)
            VALUES (:userId, :eventId, :rating, :timestamp)
            ON CONFLICT (user_id, event_id)
            DO UPDATE SET rating = GREATEST(interactions.rating, EXCLUDED.rating),
                          ts     = EXCLUDED.ts
            """, nativeQuery = true)
    void upsertInteraction(@Param("userId") long userId,
                          @Param("eventId") long eventId,
                          @Param("rating") double rating,
                          @Param("timestamp") Instant timestamp);

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
}
