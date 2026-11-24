package ru.practicum.main.stats.analyzer.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.main.stats.analyzer.model.Similarity;

import java.time.Instant;
import java.util.List;

@Repository
public interface SimilarityRepository extends JpaRepository<Similarity, Similarity.SimilarityId> {
    @Modifying
    @Transactional
    @Query(value = """
            INSERT INTO similarities ("eventA", "eventB", similarity, ts)
            VALUES (:eventA, :eventB, :score, :timestamp)
            ON CONFLICT ("eventA", "eventB")
            DO UPDATE SET similarity = EXCLUDED.similarity,
                          ts         = EXCLUDED.ts
            """, nativeQuery = true)
    void upsertSimilarity(@Param("eventA") long eventA,
                          @Param("eventB") long eventB,
                          @Param("score") double score,
                          @Param("timestamp") Instant timestamp);

    @Query("""
        SELECT s
        FROM Similarity s
        WHERE s.eventA = :eventId OR s.eventB = :eventId
        ORDER BY s.score DESC
        """)
    List<Similarity> findAllByEventId(@Param("eventId") long eventId);

    @Query("""
        SELECT s
        FROM Similarity s
        WHERE s.eventA IN :eventIds OR s.eventB IN :eventIds
        """)
    List<Similarity> findAllByEventIds(@Param("eventIds") List<Long> eventIds);

    @Query("""
        SELECT s
        FROM Similarity s
        WHERE (s.eventA = :candidate AND s.eventB IN :userEventIds)
           OR (s.eventB = :candidate AND s.eventA IN :userEventIds)
        ORDER BY s.score DESC
        """)
    List<Similarity> findNeighbors(@Param("candidate") long candidate,
                                   @Param("userEventIds") List<Long> userEventIds,
                                   Pageable pageable);
}
