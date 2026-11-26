package ru.practicum.main.stats.analyzer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.practicum.main.stats.analyzer.model.Similarity;
import java.util.List;

@Repository
public interface SimilarityRepository extends JpaRepository<Similarity, Similarity.SimilarityId> {
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
        WHERE (s.eventA IN :candidates AND s.eventB IN :userEventIds)
           OR (s.eventB IN :candidates AND s.eventA IN :userEventIds)
        ORDER BY s.score DESC
        """)
    List<Similarity> findNeighborsForCandidates(@Param("candidates") List<Long> candidates,
                                                @Param("userEventIds") List<Long> userEventIds);
}
