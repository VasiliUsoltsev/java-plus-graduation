package ru.practicum.main.comment.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.practicum.main.comment.model.EventComment;
import ru.practicum.main.comment.model.enums.EventCommentStatus;

import java.util.List;
import java.util.Optional;

@Repository
public interface EventCommentRepository extends JpaRepository<EventComment, Long> {

    Page<EventComment> findByEventAndStatus(Long eventId, EventCommentStatus status, Pageable pageable);

    Page<EventComment> findByStatus(EventCommentStatus status, Pageable pageable);

    List<EventComment> findByEventAndAuthorAndStatusOrderByCreatedDesc(
            Long eventId,
            Long userId,
            EventCommentStatus status
    );

    @Override
    Optional<EventComment> findById(Long id);

    @Override
    Page<EventComment> findAll(Pageable pageable);

    @Query("SELECT c.event, COUNT(c) FROM EventComment c " +
            "WHERE c.event IN :eventIds " +
            "GROUP BY c.event")
    List<Object[]> countByEventIds(@Param("eventIds") List<Long> eventIds);

    long countByEvent(Long eventId);
}