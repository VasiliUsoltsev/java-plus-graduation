package ru.practicum.main.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.practicum.main.model.ParticipationRequest;
import ru.practicum.main.model.enums.ParticipationStatus;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ParticipationRequestRepository extends JpaRepository<ParticipationRequest, Long> {

    List<ParticipationRequest> findAllByEventId(Long eventId);

    long countByEventIdAndStatus(Long eventId, ParticipationStatus status);

    List<ParticipationRequest> findAllByIdIn(Collection<Long> ids);

    @Query("SELECT r.event.id, COUNT(r) FROM ParticipationRequest r " +
            "WHERE r.event.id IN :eventIds AND r.status = :status " +
            "GROUP BY r.event.id")
    List<Object[]> countByEventIdsAndStatus(@Param("eventIds") List<Long> eventIds,
                                            @Param("status") ParticipationStatus status);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "UPDATE participation_requests " +
            "SET status = 'CONFIRMED' " +
            "WHERE id IN (:ids) " +
            "AND status = 'PENDING' " +
            "AND (:limit = 0 OR (" +
            "    SELECT COUNT(*) FROM participation_requests " +
            "    WHERE event_id = :eventId AND status = 'CONFIRMED'" +
            ") < :limit)", nativeQuery = true)
    int confirmRequestsAtomically(@Param("ids") List<Long> ids,
                                  @Param("eventId") Long eventId,
                                  @Param("limit") int limit);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "UPDATE participation_requests " +
            "SET status = 'REJECTED' " +
            "WHERE event_id = :eventId " +
            "AND status = 'PENDING' " +
            "AND id NOT IN (:excludeIds)", nativeQuery = true)
    int rejectRemainingPending(@Param("eventId") Long eventId,
                               @Param("excludeIds") List<Long> excludeIds);

    List<ParticipationRequest> findAllByRequesterId(Long requesterId);

    Optional<ParticipationRequest> findByEventIdAndRequesterId(Long eventId, Long requesterId);
}
