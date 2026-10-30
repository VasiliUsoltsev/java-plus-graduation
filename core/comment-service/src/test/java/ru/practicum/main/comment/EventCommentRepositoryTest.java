package ru.practicum.main.comment;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import ru.practicum.main.comment.model.EventComment;
import ru.practicum.main.comment.model.enums.EventCommentStatus;
import ru.practicum.main.comment.repository.EventCommentRepository;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class EventCommentRepositoryTest {

    @Autowired
    private EventCommentRepository eventCommentRepository;

    private Long eventId;
    private Long userId;
    private Long anotherUserId;
    private Long approvedCommentId;

    @BeforeEach
    void setUp() {
        eventId = 1L;
        userId = 1L;
        anotherUserId = 2L;

        EventComment commentWaiting = EventComment.builder()
                .content("Waiting comment")
                .author(userId)
                .event(eventId)
                .status(EventCommentStatus.WAITING)
                .created(LocalDateTime.now())
                .build();
        eventCommentRepository.save(commentWaiting);

        EventComment commentApproved = EventComment.builder()
                .content("Approved comment")
                .author(userId)
                .event(eventId)
                .status(EventCommentStatus.APPROVED)
                .created(LocalDateTime.now())
                .build();
        commentApproved = eventCommentRepository.save(commentApproved);

        EventComment commentRejected = EventComment.builder()
                .content("Rejected comment")
                .author(userId)
                .event(eventId)
                .status(EventCommentStatus.REJECTED)
                .created(LocalDateTime.now())
                .build();
        eventCommentRepository.save(commentRejected);

        approvedCommentId = commentApproved.getId();
    }

    @Test
    @DisplayName("findByEventAndStatus - должен найти WAITING комментарии по eventId")
    void findByEventAndStatus_shouldFindWaitingComments() {
        Pageable pageable = PageRequest.of(0, 10);

        Page<EventComment> page = eventCommentRepository.findByEventAndStatus(
                eventId,
                EventCommentStatus.WAITING,
                pageable
        );

        assertNotNull(page);
        assertEquals(1, page.getContent().size());
        assertEquals(EventCommentStatus.WAITING, page.getContent().getFirst().getStatus());
    }

    @Test
    @DisplayName("findByEventAndStatus - должен найти APPROVED комментарии по eventId")
    void findByEventAndStatus_shouldFindApprovedComments() {
        Pageable pageable = PageRequest.of(0, 10);

        Page<EventComment> page = eventCommentRepository.findByEventAndStatus(
                eventId,
                EventCommentStatus.APPROVED,
                pageable
        );

        assertNotNull(page);
        assertEquals(1, page.getContent().size());
        assertEquals(EventCommentStatus.APPROVED, page.getContent().getFirst().getStatus());
    }

    @Test
    @DisplayName("findByEventAndStatus - должен вернуть пустую страницу для несуществующего события")
    void findByEventAndStatus_shouldReturnEmptyPage_whenEventNotFound() {
        Pageable pageable = PageRequest.of(0, 10);

        Page<EventComment> page = eventCommentRepository.findByEventAndStatus(
                999L,
                EventCommentStatus.WAITING,
                pageable
        );

        assertNotNull(page);
        assertTrue(page.getContent().isEmpty());
        assertEquals(0, page.getTotalElements());
    }

    @Test
    @DisplayName("findByEventAndStatus - должен поддерживать пагинацию")
    void findByEventAndStatus_shouldSupportPagination() {
        EventComment anotherWaiting = EventComment.builder()
                .content("Another waiting")
                .author(userId)
                .event(eventId)
                .status(EventCommentStatus.WAITING)
                .created(LocalDateTime.now())
                .build();
        eventCommentRepository.save(anotherWaiting);

        Pageable pageable = PageRequest.of(0, 1);

        Page<EventComment> page = eventCommentRepository.findByEventAndStatus(
                eventId,
                EventCommentStatus.WAITING,
                pageable
        );

        assertNotNull(page);
        assertEquals(1, page.getContent().size());
        assertEquals(2, page.getTotalElements());
    }

    @Test
    @DisplayName("findByStatus - должен найти все WAITING комментарии")
    void findByStatus_shouldFindAllWaitingComments() {
        Pageable pageable = PageRequest.of(0, 10);

        Page<EventComment> page = eventCommentRepository.findByStatus(
                EventCommentStatus.WAITING,
                pageable
        );

        assertNotNull(page);
        assertEquals(1, page.getContent().size());
        page.getContent().forEach(c -> assertEquals(EventCommentStatus.WAITING, c.getStatus()));
    }

    @Test
    @DisplayName("findByStatus - должен найти все APPROVED комментарии")
    void findByStatus_shouldFindAllApprovedComments() {
        Pageable pageable = PageRequest.of(0, 10);

        Page<EventComment> page = eventCommentRepository.findByStatus(
                EventCommentStatus.APPROVED,
                pageable
        );

        assertNotNull(page);
        assertEquals(1, page.getContent().size());
        page.getContent().forEach(c -> assertEquals(EventCommentStatus.APPROVED, c.getStatus()));
    }

    @Test
    @DisplayName("findByStatus - должен поддерживать пагинацию")
    void findByStatus_shouldSupportPagination() {
        for (int i = 0; i < 2; i++) {
            EventComment additional = EventComment.builder()
                    .content("Additional " + i)
                    .author(userId)
                    .event(eventId)
                    .status(EventCommentStatus.WAITING)
                    .created(LocalDateTime.now())
                    .build();
            eventCommentRepository.save(additional);
        }

        Pageable pageable = PageRequest.of(0, 2);

        Page<EventComment> page = eventCommentRepository.findByStatus(
                EventCommentStatus.WAITING,
                pageable
        );

        assertNotNull(page);
        assertEquals(2, page.getContent().size());
        assertEquals(3, page.getTotalElements());
    }

    @Test
    @DisplayName("findByEventAndAuthorAndStatusOrderByCreatedDesc - фильтрует по APPROVED")
    void findByEventAndAuthorAndStatus_shouldFilterApproved() {
        List<EventComment> comments = eventCommentRepository
                .findByEventAndAuthorAndStatusOrderByCreatedDesc(
                        eventId,
                        userId,
                        EventCommentStatus.APPROVED
                );

        assertEquals(1, comments.size());
        assertEquals("Approved comment", comments.getFirst().getContent());
        assertEquals(EventCommentStatus.APPROVED, comments.getFirst().getStatus());
    }

    @Test
    @DisplayName("findByEventAndAuthorAndStatusOrderByCreatedDesc - не возвращает комментарии другого автора")
    void findByEventAndAuthorAndStatus_shouldNotReturnOtherAuthorComments() {
        EventComment otherApproved = EventComment.builder()
                .content("Other author approved")
                .author(anotherUserId)
                .event(eventId)
                .status(EventCommentStatus.APPROVED)
                .created(LocalDateTime.now())
                .build();
        eventCommentRepository.save(otherApproved);

        List<EventComment> comments = eventCommentRepository
                .findByEventAndAuthorAndStatusOrderByCreatedDesc(
                        eventId,
                        userId,
                        EventCommentStatus.APPROVED
                );

        assertEquals(1, comments.size());
        assertEquals("Approved comment", comments.getFirst().getContent());
        assertEquals(userId, comments.getFirst().getAuthor());
    }

    @Test
    @DisplayName("findById - возвращает комментарий по ID")
    void findById_shouldReturnComment() {
        EventComment loaded = eventCommentRepository.findById(approvedCommentId).orElseThrow();

        assertNotNull(loaded);
        assertEquals("Approved comment", loaded.getContent());
        assertEquals(EventCommentStatus.APPROVED, loaded.getStatus());
    }

    @Test
    @DisplayName("countByEvent - считает комментарии по eventId")
    void countByEvent_shouldCountComments() {
        long count = eventCommentRepository.countByEvent(eventId);
        assertEquals(3, count);
    }

    @Test
    @DisplayName("countByEventIds - группирует количество по eventId")
    void countByEventIds_shouldGroupCounts() {
        List<Object[]> result = eventCommentRepository.countByEventIds(List.of(eventId));

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(eventId, result.getFirst()[0]);
        assertEquals(3L, result.getFirst()[1]);
    }
}