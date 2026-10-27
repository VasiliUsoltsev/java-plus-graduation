package ru.practicum.main.request.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.practicum.main.request.model.ParticipationRequest;
import ru.practicum.main.request.model.enums.ParticipationStatus;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ParticipationRequestRepository extends JpaRepository<ParticipationRequest, Long> {

    List<ParticipationRequest> findAllByEvent(Long eventId);

    long countByEventAndStatus(Long eventId, ParticipationStatus status);

    List<ParticipationRequest> findAllByIdIn(Collection<Long> ids);

    @Query("SELECT r.event, COUNT(r) FROM ParticipationRequest r " +
            "WHERE r.event IN :eventIds AND r.status = :status " +
            "GROUP BY r.event")
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

    List<ParticipationRequest> findAllByRequester(Long requesterId);

    Optional<ParticipationRequest> findByEventAndRequester(Long eventId, Long requesterId);
}