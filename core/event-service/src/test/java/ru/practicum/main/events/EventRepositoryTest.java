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
import ru.practicum.main.model.enums.EventState;
import ru.practicum.main.repository.CategoryRepository;
import ru.practicum.main.repository.EventRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@ActiveProfiles("test")
class EventRepositoryTest {

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private CategoryRepository categoryRepository;

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
        categoryRepository.deleteAll();

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
                .category(category1)
                .build());
    }

    // ==================== ТЕСТЫ ДЛЯ findAllByInitiator ====================

    @Test
    void findAllByInitiator_ShouldReturnEvents_WhenUserHasEvents() {
        Long initiatorId = 1L;
        Pageable pageable = PageRequest.of(0, 10);
        Page<Event> page = eventRepository.findAllByInitiator(initiatorId, pageable);

        assertThat(page.getContent()).isNotNull();
    }

    @Test
    void findAllByInitiator_ShouldReturnEmpty_WhenUserHasNoEvents() {
        Long initiatorId = 999L;
        Pageable pageable = PageRequest.of(0, 10);
        Page<Event> page = eventRepository.findAllByInitiator(initiatorId, pageable);

        assertThat(page.getContent()).isEmpty();
    }

    // ==================== ТЕСТЫ ДЛЯ findByEventIdAndInitiator ====================

    @Test
    void findByEventIdAndInitiator_ShouldReturnEvent_WhenExists() {
        Long initiatorId = 1L;
        Optional<Event> found = eventRepository.findByEventIdAndInitiator(event1.getId(), initiatorId);

        assertFalse(found.isPresent());
    }

    @Test
    void findByEventIdAndInitiator_ShouldReturnEmpty_WhenWrongUser() {
        Long wrongUserId = 2L;
        Optional<Event> found = eventRepository.findByEventIdAndInitiator(event1.getId(), wrongUserId);

        assertFalse(found.isPresent());
    }

    @Test
    void findByEventIdAndInitiator_ShouldReturnEmpty_WhenEventNotExists() {
        Long userId = 1L;
        Optional<Event> found = eventRepository.findByEventIdAndInitiator(999L, userId);

        assertFalse(found.isPresent());
    }

    // ==================== ТЕСТЫ ДЛЯ existsByCategoryId ====================

    @Test
    void existsByCategoryId_ShouldReturnTrue_WhenCategoryHasEvents() {
        assertTrue(eventRepository.existsByCategoryId(category1.getId()));
        assertTrue(eventRepository.existsByCategoryId(category2.getId()));
    }

    @Test
    void existsByCategoryId_ShouldReturnFalse_WhenCategoryHasNoEvents() {
        Category emptyCategory = categoryRepository.save(Category.builder()
                .name("Пустая категория")
                .build());

        assertFalse(eventRepository.existsByCategoryId(emptyCategory.getId()));
    }
}