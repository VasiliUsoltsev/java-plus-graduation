package ru.practicum.main.events;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import ru.practicum.main.config.QuerydslConfig;
import ru.practicum.main.dto.request.AdminEventSearchFilter;
import ru.practicum.main.dto.request.PublicEventSearchFilter;
import ru.practicum.main.model.Category;
import ru.practicum.main.model.Event;
import ru.practicum.main.model.Location;
import ru.practicum.main.model.User;
import ru.practicum.main.model.enums.EventState;
import ru.practicum.main.repository.CategoryRepository;
import ru.practicum.main.repository.EventRepository;
import ru.practicum.main.repository.EventSearchRepository;
import ru.practicum.main.repository.UserRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Import({EventSearchRepository.class, QuerydslConfig.class})
class EventSearchRepositoryTest {

    @Autowired
    private EventSearchRepository eventSearchRepository;

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

    @Test
    void searchAdmin_ShouldReturnAllEvents_WhenNoFilters() {
        AdminEventSearchFilter filter = new AdminEventSearchFilter(
                null, null, null, baseTime.minusDays(10), baseTime.plusDays(100));

        Page<Event> page = eventSearchRepository.searchAdmin(filter, PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(4);
    }

    @Test
    void searchAdmin_ShouldFilterByUsers() {
        AdminEventSearchFilter filter = new AdminEventSearchFilter(
                List.of(user1.getId()), null, null, baseTime.minusDays(10), baseTime.plusDays(100));

        Page<Event> page = eventSearchRepository.searchAdmin(filter, PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getContent()).extracting(Event::getTitle)
                .containsExactlyInAnyOrder("Концерт в парке", "Джазовый вечер");
    }

    @Test
    void searchAdmin_ShouldFilterByStates() {
        AdminEventSearchFilter filter = new AdminEventSearchFilter(
                null, List.of(EventState.PUBLISHED), null, baseTime.minusDays(10), baseTime.plusDays(100));

        Page<Event> page = eventSearchRepository.searchAdmin(filter, PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getContent()).extracting(Event::getState)
                .allMatch(state -> state == EventState.PUBLISHED);
    }

    @Test
    void searchAdmin_ShouldFilterByCategories() {
        AdminEventSearchFilter filter = new AdminEventSearchFilter(
                null, null, List.of(category2.getId()), baseTime.minusDays(10), baseTime.plusDays(100));

        Page<Event> page = eventSearchRepository.searchAdmin(filter, PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().getFirst().getTitle()).isEqualTo("Арт-выставка");
    }

    @Test
    void searchAdmin_ShouldFilterByDateRange() {
        AdminEventSearchFilter filter = new AdminEventSearchFilter(
                null, null, null, baseTime.plusDays(14), baseTime.plusDays(25));

        Page<Event> page = eventSearchRepository.searchAdmin(filter, PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getContent()).extracting(Event::getTitle)
                .containsExactlyInAnyOrder("Арт-выставка", "Семейный спектакль");
    }

    @Test
    void searchAdmin_ShouldReturnEmpty_WhenNoMatch() {
        AdminEventSearchFilter filter = new AdminEventSearchFilter(
                List.of(user1.getId()), List.of(EventState.PUBLISHED),
                List.of(category2.getId()), baseTime.minusDays(10), baseTime.plusDays(100));

        Page<Event> page = eventSearchRepository.searchAdmin(filter, PageRequest.of(0, 10));

        assertThat(page.getContent()).isEmpty();
    }

    @Test
    void searchPublic_ShouldReturnPublishedEvents() {
        PublicEventSearchFilter filter = new PublicEventSearchFilter(
                null, EventState.PUBLISHED, null, null,
                baseTime.minusDays(1), baseTime.plusDays(100), null);

        Page<Event> page = eventSearchRepository.searchPublic(filter, PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getContent()).extracting(Event::getTitle)
                .containsExactlyInAnyOrder("Концерт в парке", "Семейный спектакль");
    }

    @Test
    void searchPublic_ShouldSearchByTextInAnnotation() {
        PublicEventSearchFilter filter = new PublicEventSearchFilter(
                "музыки", EventState.PUBLISHED, null, null,
                baseTime.minusDays(1), baseTime.plusDays(100), null);

        Page<Event> page = eventSearchRepository.searchPublic(filter, PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().getFirst().getTitle()).isEqualTo("Концерт в парке");
    }

    @Test
    void searchPublic_ShouldSearchByTextInDescription() {
        PublicEventSearchFilter filter = new PublicEventSearchFilter(
                "детям", EventState.PUBLISHED, null, null,
                baseTime.minusDays(1), baseTime.plusDays(100), null);

        Page<Event> page = eventSearchRepository.searchPublic(filter, PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().getFirst().getTitle()).isEqualTo("Семейный спектакль");
    }

    @Test
    void searchPublic_ShouldFilterByCategory() {
        PublicEventSearchFilter filter = new PublicEventSearchFilter(
                null, EventState.PUBLISHED, List.of(category2.getId()), null,
                baseTime.minusDays(1), baseTime.plusDays(100), null);

        Page<Event> page = eventSearchRepository.searchPublic(filter, PageRequest.of(0, 10));

        assertThat(page.getContent()).isEmpty();
    }

    @Test
    void searchPublic_ShouldFilterByPaid() {
        PublicEventSearchFilter filter = new PublicEventSearchFilter(
                null, EventState.PUBLISHED, null, true,
                baseTime.minusDays(1), baseTime.plusDays(100), null);

        Page<Event> page = eventSearchRepository.searchPublic(filter, PageRequest.of(0, 10));

        assertThat(page.getContent()).isEmpty();
    }

    @Test
    void searchPublic_ShouldFilterByFreeEvents() {
        PublicEventSearchFilter filter = new PublicEventSearchFilter(
                null, EventState.PUBLISHED, null, false,
                baseTime.minusDays(1), baseTime.plusDays(100), null);

        Page<Event> page = eventSearchRepository.searchPublic(filter, PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getContent()).extracting(Event::getPaid)
                .allMatch(paid -> !paid);
    }

    @Test
    void searchPublic_ShouldRespectPagination() {
        PublicEventSearchFilter filter = new PublicEventSearchFilter(
                null, EventState.PUBLISHED, null, null,
                baseTime.minusDays(1), baseTime.plusDays(100), null);

        Page<Event> page = eventSearchRepository.searchPublic(filter, PageRequest.of(0, 1));

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getTotalPages()).isEqualTo(2);
    }

    @Test
    void searchPublic_ShouldFilterOnlyAvailable_WhenOnlyAvailableTrue() {
        PublicEventSearchFilter filter = new PublicEventSearchFilter(
                null, EventState.PUBLISHED, null, null,
                baseTime.minusDays(1), baseTime.plusDays(100), true);

        Page<Event> page = eventSearchRepository.searchPublic(filter, PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getContent()).extracting(Event::getTitle)
                .containsExactlyInAnyOrder("Концерт в парке", "Семейный спектакль");
    }
}
