package ru.practicum.main.compilations;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.main.model.Category;
import ru.practicum.main.model.Compilation;
import ru.practicum.main.model.Event;
import ru.practicum.main.model.Location;
import ru.practicum.main.model.enums.EventState;
import ru.practicum.main.repository.CompilationRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Transactional
class CompilationRepositoryTest {

    @Autowired
    private CompilationRepository compilationRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Compilation compilation1;
    private Compilation compilation2;
    private Compilation compilation3;
    private Event event;
    private Category category;
    private Location location;

    @BeforeEach
    void setUp() {
        category = Category.builder()
                .name("Test Category")
                .build();
        entityManager.persist(category);

        location = Location.builder()
                .lat(55.7558f)
                .lon(37.6173f)
                .build();

        event = Event.builder()
                .annotation("Test Annotation")
                .description("Test Description")
                .title("Test Event")
                .paid(false)
                .eventDate(LocalDateTime.now().plusDays(1))
                .createdOn(LocalDateTime.now())
                .state(EventState.PENDING)
                .participantLimit(0)
                .requestModeration(true)
                .category(category)
                .location(location)
                .build();
        entityManager.persist(event);
        entityManager.flush();

        List<Event> events1 = new ArrayList<>();
        events1.add(event);

        List<Event> events2 = new ArrayList<>();
        events2.add(event);

        List<Event> events3 = new ArrayList<>();
        events3.add(event);

        compilation1 = Compilation.builder()
                .title("Pinned Compilation 1")
                .pinned(true)
                .events(events1)
                .build();

        compilation2 = Compilation.builder()
                .title("Pinned Compilation 2")
                .pinned(true)
                .events(events2)
                .build();

        compilation3 = Compilation.builder()
                .title("Not Pinned Compilation")
                .pinned(false)
                .events(events3)
                .build();

        compilation1 = entityManager.persist(compilation1);
        compilation2 = entityManager.persist(compilation2);
        compilation3 = entityManager.persist(compilation3);
        entityManager.flush();
    }

    @Test
    void findByPinned_ShouldReturnOnlyPinnedCompilations() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Compilation> result = compilationRepository.findByPinned(true, pageable);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent())
                .extracting(Compilation::getPinned)
                .containsOnly(true);
        assertThat(result.getContent())
                .extracting(Compilation::getTitle)
                .containsExactlyInAnyOrder("Pinned Compilation 1", "Pinned Compilation 2");
    }

    @Test
    void findByPinned_WithPinnedFalse_ShouldReturnOnlyNotPinnedCompilations() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Compilation> result = compilationRepository.findByPinned(false, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getPinned()).isFalse();
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("Not Pinned Compilation");
    }

    @Test
    void findAll_ShouldReturnAllCompilations() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Compilation> result = compilationRepository.findAll(pageable);

        assertThat(result.getContent()).hasSize(3);
        assertThat(result.getContent())
                .extracting(Compilation::getTitle)
                .containsExactlyInAnyOrder(
                        "Pinned Compilation 1",
                        "Pinned Compilation 2",
                        "Not Pinned Compilation"
                );
    }

    @Test
    void findByPinned_WithPagination_ShouldReturnCorrectPage() {
        Pageable pageable = PageRequest.of(0, 1);
        Page<Compilation> result = compilationRepository.findByPinned(true, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getTotalPages()).isEqualTo(2);
    }

    @Test
    void save_ShouldPersistCompilationWithEvents() {
        Location newLocation = Location.builder()
                .lat(55.7558f)
                .lon(37.6173f)
                .build();

        Event newEvent = Event.builder()
                .annotation("New Event Annotation")
                .description("New Event Description")
                .title("New Event")
                .paid(true)
                .eventDate(LocalDateTime.now().plusDays(2))
                .createdOn(LocalDateTime.now())
                .state(EventState.PENDING)
                .participantLimit(10)
                .requestModeration(true)
                .category(category)
                .location(newLocation)
                .build();
        entityManager.persist(newEvent);
        entityManager.flush();

        List<Event> newEvents = new ArrayList<>();
        newEvents.add(newEvent);

        Compilation newCompilation = Compilation.builder()
                .title("New Compilation")
                .pinned(true)
                .events(newEvents)
                .build();

        Compilation saved = compilationRepository.save(newCompilation);
        entityManager.flush();

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getTitle()).isEqualTo("New Compilation");
        assertThat(saved.getEvents()).hasSize(1);
        assertThat(saved.getEvents().get(0).getTitle()).isEqualTo("New Event");
    }

    @Test
    void delete_ShouldRemoveCompilationAndRelations() {
        Long compilationId = compilation1.getId();

        compilationRepository.deleteById(compilationId);
        entityManager.flush();

        Compilation found = compilationRepository.findById(compilationId).orElse(null);
        assertThat(found).isNull();

        Event eventStillExists = entityManager.find(Event.class, event.getId());
        assertThat(eventStillExists).isNotNull();
    }

    @Test
    void update_ShouldModifyExistingCompilation() {
        Long compilationId = compilation1.getId();

        Compilation compilationToUpdate = compilationRepository.findById(compilationId)
                .orElseThrow(() -> new AssertionError("Compilation not found"));

        compilationToUpdate.setTitle("Updated Title");
        compilationToUpdate.setPinned(false);

        Compilation updated = compilationRepository.save(compilationToUpdate);
        entityManager.flush();

        Compilation found = compilationRepository.findById(compilationId)
                .orElseThrow(() -> new AssertionError("Compilation not found after update"));

        assertThat(found.getTitle()).isEqualTo("Updated Title");
        assertThat(found.getPinned()).isFalse();

        assertThat(found.getEvents()).isNotEmpty();
    }

    @Test
    void update_ShouldClearEventsWhenEmptyListProvided() {
        Long compilationId = compilation1.getId();

        Compilation compilationToUpdate = compilationRepository.findById(compilationId)
                .orElseThrow(() -> new AssertionError("Compilation not found"));

        compilationToUpdate.setEvents(new ArrayList<>());

        Compilation updated = compilationRepository.save(compilationToUpdate);
        entityManager.flush();

        Compilation found = compilationRepository.findById(compilationId)
                .orElseThrow(() -> new AssertionError("Compilation not found after update"));

        assertThat(found.getEvents()).isEmpty();
    }

    @Test
    void update_ShouldAddNewEvents() {
        Location newLocation = Location.builder()
                .lat(55.7558f)
                .lon(37.6173f)
                .build();

        Event newEvent = Event.builder()
                .annotation("New Event Annotation")
                .description("New Event Description")
                .title("New Event")
                .paid(true)
                .eventDate(LocalDateTime.now().plusDays(2))
                .createdOn(LocalDateTime.now())
                .state(EventState.PENDING)
                .participantLimit(10)
                .requestModeration(true)
                .category(category)
                .location(newLocation)
                .build();
        entityManager.persist(newEvent);
        entityManager.flush();

        Long compilationId = compilation1.getId();

        Compilation compilationToUpdate = compilationRepository.findById(compilationId)
                .orElseThrow(() -> new AssertionError("Compilation not found"));

        List<Event> updatedEvents = new ArrayList<>(compilationToUpdate.getEvents());
        updatedEvents.add(newEvent);
        compilationToUpdate.setEvents(updatedEvents);

        Compilation updated = compilationRepository.save(compilationToUpdate);
        entityManager.flush();

        Compilation found = compilationRepository.findById(compilationId)
                .orElseThrow(() -> new AssertionError("Compilation not found after update"));

        assertThat(found.getEvents()).hasSize(2);
        assertThat(found.getEvents()).extracting(Event::getTitle)
                .containsExactlyInAnyOrder("Test Event", "New Event");
    }

    @Test
    void existsByTitle_ShouldReturnTrueWhenCompilationExists() {
        assertThat(compilationRepository.existsByTitle("Pinned Compilation 1")).isTrue();
        assertThat(compilationRepository.existsByTitle("Not Pinned Compilation")).isTrue();
    }

    @Test
    void existsByTitle_ShouldReturnFalseWhenCompilationDoesNotExist() {
        assertThat(compilationRepository.existsByTitle("Unknown Compilation")).isFalse();
    }

    @Test
    void existsByTitleAndIdNot_ShouldReturnTrueWhenTitleExistsForOtherCompilation() {
        assertThat(compilationRepository.existsByTitleAndIdNot("Pinned Compilation 1", compilation2.getId())).isTrue();
        assertThat(compilationRepository.existsByTitleAndIdNot("Pinned Compilation 1", compilation3.getId())).isTrue();
    }

    @Test
    void existsByTitleAndIdNot_ShouldReturnFalseWhenTitleBelongsToSameCompilation() {
        assertThat(compilationRepository.existsByTitleAndIdNot("Pinned Compilation 1", compilation1.getId())).isFalse();
    }

    @Test
    void existsByTitleAndIdNot_ShouldReturnFalseWhenTitleDoesNotExist() {
        assertThat(compilationRepository.existsByTitleAndIdNot("Unknown Compilation", compilation1.getId())).isFalse();
    }
}