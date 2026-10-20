package ru.practicum.main.events;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import ru.practicum.main.model.Category;
import ru.practicum.main.model.Event;
import ru.practicum.main.model.Location;
import ru.practicum.main.model.User;
import ru.practicum.main.model.enums.EventState;
import ru.practicum.main.repository.CategoryRepository;
import ru.practicum.main.repository.EventRepository;
import ru.practicum.main.repository.UserRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class EventRepositoryTest {

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private User user1;
    private User user2;
    private Category category1;
    private Category category2;
    private Event event1;
    private Event event2;
    private Event event3;
    private Event event4;

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private LocalDateTime baseTime;

    @BeforeEach
    void setUp() {
        baseTime = LocalDateTime.parse("2026-08-27 00:00:00", formatter);
        eventRepository.deleteAll();
        userRepository.deleteAll();
        categoryRepository.deleteAll();

        user1 = userRepository.save(User.builder()
                .name("John Doe")
                .email("john@example.com")
                .build());

        user2 = userRepository.save(User.builder()
                .name("Jane Smith")
                .email("jane@example.com")
                .build());

        category1 = categoryRepository.save(Category.builder()
                .name("Концерты")
                .build());

        category2 = categoryRepository.save(Category.builder()
                .name("Выставки")
                .build());

        event1 = eventRepository.save(Event.builder()
                .annotation("Концерт классической музыки в парке")
                .description("Приглашаем вас на великолепный концерт классической музыки")
                .eventDate(baseTime.plusDays(10))
                .createdOn(baseTime.minusDays(5))
                .paid(false)
                .participantLimit(0)
                .requestModeration(true)
                .title("Концерт в парке")
                .state(EventState.PUBLISHED)
                .location(Location.builder().lat(55.75f).lon(37.62f).build())
                .initiator(user1)
                .category(category1)
                .build());

        event2 = eventRepository.save(Event.builder()
                .annotation("Выставка современного искусства")
                .description("Лучшие работы современных художников")
                .eventDate(baseTime.plusDays(20))
                .createdOn(baseTime.minusDays(3))
                .paid(true)
                .participantLimit(50)
                .requestModeration(true)
                .title("Арт-выставка")
                .state(EventState.PENDING)
                .location(Location.builder().lat(55.76f).lon(37.63f).build())
                .initiator(user2)
                .category(category2)
                .build());

        event3 = eventRepository.save(Event.builder()
                .annotation("Джазовый вечер в ресторане")
                .description("Вечер живой джазовой музыки с ужином")
                .eventDate(baseTime.plusDays(5))
                .createdOn(baseTime.minusDays(1))
                .paid(true)
                .participantLimit(30)
                .requestModeration(false)
                .title("Джазовый вечер")
                .state(EventState.CANCELED)
                .location(Location.builder().lat(55.77f).lon(37.64f).build())
                .initiator(user1)
                .category(category1)
                .build());

        event4 = eventRepository.save(Event.builder()
                .annotation("Театральный спектакль для всей семьи")
                .description("Весёлый спектакль который понравится детям и взрослым")
                .eventDate(baseTime.plusDays(15))
                .createdOn(baseTime.minusDays(2))
                .paid(false)
                .participantLimit(100)
                .requestModeration(true)
                .title("Семейный спектакль")
                .state(EventState.PUBLISHED)
                .location(Location.builder().lat(55.78f).lon(37.65f).build())
                .initiator(user2)
                .category(category1)
                .build());
    }

    // ==================== ТЕСТЫ ДЛЯ findAllByInitiatorId ====================

    @Test
    void findAllByInitiatorId_ShouldReturnEvents_WhenUserHasEvents() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Event> page = eventRepository.findAllByInitiatorId(user1.getId(), pageable);

        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getContent()).extracting(Event::getTitle)
                .containsExactlyInAnyOrder("Концерт в парке", "Джазовый вечер");
    }

    @Test
    void findAllByInitiatorId_ShouldReturnEmpty_WhenUserHasNoEvents() {
        User newUser = userRepository.save(User.builder()
                .name("No Events User")
                .email("noevents@example.com")
                .build());

        Pageable pageable = PageRequest.of(0, 10);
        Page<Event> page = eventRepository.findAllByInitiatorId(newUser.getId(), pageable);

        assertThat(page.getContent()).isEmpty();
    }

    @Test
    void findAllByInitiatorId_ShouldRespectPagination() {
        Pageable pageable = PageRequest.of(0, 1);
        Page<Event> page = eventRepository.findAllByInitiatorId(user1.getId(), pageable);

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getTotalElements()).isEqualTo(2);
    }

    // ==================== ТЕСТЫ ДЛЯ findByEventIdAndInitiatorId ====================

    @Test
    void findByEventIdAndInitiatorId_ShouldReturnEvent_WhenExists() {
        Optional<Event> found = eventRepository.findByEventIdAndInitiatorId(event1.getId(), user1.getId());

        assertTrue(found.isPresent());
        assertEquals("Концерт в парке", found.get().getTitle());
    }

    @Test
    void findByEventIdAndInitiatorId_ShouldReturnEmpty_WhenWrongUser() {
        Optional<Event> found = eventRepository.findByEventIdAndInitiatorId(event1.getId(), user2.getId());

        assertFalse(found.isPresent());
    }

    @Test
    void findByEventIdAndInitiatorId_ShouldReturnEmpty_WhenEventNotExists() {
        Optional<Event> found = eventRepository.findByEventIdAndInitiatorId(999L, user1.getId());

        assertFalse(found.isPresent());
    }

}
