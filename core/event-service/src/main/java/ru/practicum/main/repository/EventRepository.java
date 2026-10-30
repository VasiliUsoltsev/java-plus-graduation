package ru.practicum.main.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.practicum.main.model.Event;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {

    @EntityGraph(attributePaths = {"category"})
    Page<Event> findAllByInitiator(Long userId, Pageable pageable);

    @Query("SELECT e FROM Event e WHERE e.id = :eventId AND e.initiator = :userId")
    Optional<Event> findByEventIdAndInitiator(@Param("eventId") Long eventId,
                                              @Param("userId") Long userId);

    boolean existsByCategoryId(Long categoryId);

    @Query(value = "SELECT id, title FROM events WHERE id IN (:ids)", nativeQuery = true)
    List<Object[]> findShortInfoByIds(@Param("ids") Collection<Long> ids);
}