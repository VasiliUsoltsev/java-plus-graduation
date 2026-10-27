package ru.practicum.main.EventComment;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import ru.practicum.main.model.*;
import ru.practicum.main.model.enums.EventCommentStatus;
import ru.practicum.main.model.enums.EventState;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class EventCommentRepositoryTest {

    @Autowired
    private ru.practicum.main.repository.EventCommentRepository eventCommentRepository;

    @Autowired
    private TestEntityManager entityManager;

    private User user;
    private Category category;
    private Location location;
    private Event event;
    private Long approvedCommentId;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .name("Test User")
                .email("test@example.com")
                .build();
        entityManager.persist(user);

        category = Category.builder()
                .name("Test Category")
                .build();
        entityManager.persist(category);

        location = Location.builder()
                .lat(55.75f)
                .lon(37.61f)
                .build();

        event = Event.builder()
                .annotation("Test annotation")
                .description("Test description")
                .eventDate(LocalDateTime.now().plusDays(1))
                .createdOn(LocalDateTime.now())
                .publishedOn(null)
                .paid(false)
                .participantLimit(10)
                .requestModeration(true)
                .title("Test Event")
                .state(EventState.PENDING)
                .location(location)
                .initiator(user)
                .category(category)
                .build();
        entityManager.persist(event);

        EventComment commentWaiting = EventComment.builder()
                .content("Waiting comment")
                .author(user)
                .event(event)
                .status(EventCommentStatus.WAITING)
                .created(LocalDateTime.now())
                .build();
        entityManager.persist(commentWaiting);

        EventComment commentApproved = EventComment.builder()
                .content("Approved comment")
                .author(user)
                .event(event)
                .status(EventCommentStatus.APPROVED)
                .created(LocalDateTime.now())
                .build();
        entityManager.persist(commentApproved);

        EventComment commentRejected = EventComment.builder()
                .content("Rejected comment")
                .author(user)
                .event(event)
                .status(EventCommentStatus.REJECTED)
                .created(LocalDateTime.now())
                .build();
        entityManager.persist(commentRejected);

        entityManager.flush();

        approvedCommentId = commentApproved.getId();
    }

    @Test
    @DisplayName("findByEventIdAndStatus - должен найти WAITING комментарии по eventId")
    void findByEventIdAndStatus_shouldFindWaitingComments() {
        Pageable pageable = PageRequest.of(0, 10);

        Page<EventComment> page = eventCommentRepository.findByEventIdAndStatus(
                event.getId(),
                EventCommentStatus.WAITING,
                pageable
        );

        assertNotNull(page);
        assertEquals(1, page.getContent().size());
        assertEquals(EventCommentStatus.WAITING, page.getContent().getFirst().getStatus());
    }

    @Test
    @DisplayName("findByEventIdAndStatus - должен найти APPROVED комментарии по eventId")
    void findByEventIdAndStatus_shouldFindApprovedComments() {
        Pageable pageable = PageRequest.of(0, 10);

        Page<EventComment> page = eventCommentRepository.findByEventIdAndStatus(
                event.getId(),
                EventCommentStatus.APPROVED,
                pageable
        );

        assertNotNull(page);
        assertEquals(1, page.getContent().size());
        assertEquals(EventCommentStatus.APPROVED, page.getContent().getFirst().getStatus());
    }

    @Test
    @DisplayName("findByEventIdAndStatus - должен вернуть пустую страницу для несуществующего события")
    void findByEventIdAndStatus_shouldReturnEmptyPage_whenEventNotFound() {
        Pageable pageable = PageRequest.of(0, 10);

        Page<EventComment> page = eventCommentRepository.findByEventIdAndStatus(
                999L,
                EventCommentStatus.WAITING,
                pageable
        );

        assertNotNull(page);
        assertTrue(page.getContent().isEmpty());
        assertEquals(0, page.getTotalElements());
    }

    @Test
    @DisplayName("findByEventIdAndStatus - должен поддерживать пагинацию")
    void findByEventIdAndStatus_shouldSupportPagination() {
        // Добавляем ещё один WAITING комментарий
        EventComment anotherWaiting = EventComment.builder()
                .content("Another waiting")
                .author(user)
                .event(event)
                .status(EventCommentStatus.WAITING)
                .created(LocalDateTime.now())
                .build();
        entityManager.persist(anotherWaiting);
        entityManager.flush();

        Pageable pageable = PageRequest.of(0, 1);

        Page<EventComment> page = eventCommentRepository.findByEventIdAndStatus(
                event.getId(),
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
        // Добавляем ещё два WAITING комментария
        for (int i = 0; i < 2; i++) {
            EventComment additional = EventComment.builder()
                    .content("Additional " + i)
                    .author(user)
                    .event(event)
                    .status(EventCommentStatus.WAITING)
                    .created(LocalDateTime.now())
                    .build();
            entityManager.persist(additional);
        }
        entityManager.flush();

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
    @DisplayName("findById - загружает author и event одним запросом (после detach нет LazyInitializationException)")
    void findById_shouldEagerlyFetchAuthorAndEvent() {
        EventComment loaded = eventCommentRepository.findById(approvedCommentId).orElseThrow();

        entityManager.clear();

        assertNotNull(loaded.getAuthor().getName());
        assertNotNull(loaded.getEvent().getTitle());
    }

    @Test
    @DisplayName("findByEventIdAndAuthorIdAndStatusOrderByCreatedDesc - фильтрует по APPROVED и загружает author/event")
    void findByEventIdAndAuthorIdAndStatus_shouldFilterApprovedAndFetchAssociations() {
        List<EventComment> comments = eventCommentRepository
                .findByEventIdAndAuthorIdAndStatusOrderByCreatedDesc(
                        event.getId(),
                        user.getId(),
                        EventCommentStatus.APPROVED
                );

        assertEquals(1, comments.size());
        assertEquals("Approved comment", comments.getFirst().getContent());
        assertEquals(EventCommentStatus.APPROVED, comments.getFirst().getStatus());

        entityManager.clear();

        comments.forEach(c -> {
            assertNotNull(c.getAuthor().getName());
            assertNotNull(c.getEvent().getTitle());
        });
    }

    @Test
    @DisplayName("findByEventIdAndAuthorIdAndStatusOrderByCreatedDesc - не возвращает комментарии другого автора")
    void findByEventIdAndAuthorIdAndStatus_shouldNotReturnOtherAuthorComments() {
        User anotherUser = User.builder()
                .name("Another User")
                .email("another@example.com")
                .build();
        entityManager.persist(anotherUser);

        EventComment otherApproved = EventComment.builder()
                .content("Other author approved")
                .author(anotherUser)
                .event(event)
                .status(EventCommentStatus.APPROVED)
                .created(LocalDateTime.now())
                .build();
        entityManager.persist(otherApproved);
        entityManager.flush();

        List<EventComment> comments = eventCommentRepository
                .findByEventIdAndAuthorIdAndStatusOrderByCreatedDesc(
                        event.getId(),
                        user.getId(),
                        EventCommentStatus.APPROVED
                );

        assertEquals(1, comments.size());
        assertEquals("Approved comment", comments.getFirst().getContent());
        assertEquals(user.getId(), comments.getFirst().getAuthor().getId());
    }
}
