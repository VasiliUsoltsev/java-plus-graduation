package ru.practicum.main.requests;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import ru.practicum.main.model.*;
import ru.practicum.main.model.enums.EventState;
import ru.practicum.main.model.enums.ParticipationStatus;
import ru.practicum.main.repository.CategoryRepository;
import ru.practicum.main.repository.EventRepository;
import ru.practicum.main.repository.ParticipationRequestRepository;
import ru.practicum.main.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class ParticipationRequestRepositoryTest {

    @Autowired
    private ParticipationRequestRepository requestRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private User user1;
    private User user2;
    private Category category;
    private Event event;

    @BeforeEach
    void setUp() {
        requestRepository.deleteAll();
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

        category = categoryRepository.save(Category.builder()
                .name("Концерты")
                .build());

        Location location = Location.builder()
                .lat(55.75f)
                .lon(37.62f)
                .build();

        event = eventRepository.save(Event.builder()
                .annotation("Test event")
                .description("Test description")
                .title("Test Event")
                .state(EventState.PUBLISHED)
                .initiator(user1)
                .participantLimit(0)
                .requestModeration(true)
                .category(category)
                .eventDate(LocalDateTime.now().plusDays(5))
                .createdOn(LocalDateTime.now())
                .paid(false)
                .location(location)
                .build());
    }

    @Test
    void findAllByRequesterId_ShouldReturnRequests_WhenExist() {
        ParticipationRequest request1 = ParticipationRequest.builder()
                .created(LocalDateTime.now())
                .status(ParticipationStatus.PENDING)
                .event(event)
                .requester(user2)
                .build();

        ParticipationRequest request2 = ParticipationRequest.builder()
                .created(LocalDateTime.now())
                .status(ParticipationStatus.CONFIRMED)
                .event(event)
                .requester(user2)
                .build();

        requestRepository.saveAll(List.of(request1, request2));

        List<ParticipationRequest> found = requestRepository.findAllByRequesterId(user2.getId());

        assertThat(found).hasSize(2);
        assertThat(found).extracting(ParticipationRequest::getRequester)
                .allMatch(u -> u.getId().equals(user2.getId()));
    }

    @Test
    void findAllByRequesterId_ShouldReturnEmpty_WhenNoRequests() {
        List<ParticipationRequest> found = requestRepository.findAllByRequesterId(user2.getId());
        assertThat(found).isEmpty();
    }

    @Test
    void findByEventIdAndRequesterId_ShouldReturnRequest_WhenExists() {
        ParticipationRequest request = ParticipationRequest.builder()
                .created(LocalDateTime.now())
                .status(ParticipationStatus.PENDING)
                .event(event)
                .requester(user2)
                .build();

        requestRepository.save(request);

        Optional<ParticipationRequest> found = requestRepository.findByEventIdAndRequesterId(event.getId(), user2.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getEvent().getId()).isEqualTo(event.getId());
        assertThat(found.get().getRequester().getId()).isEqualTo(user2.getId());
    }

    @Test
    void findByEventIdAndRequesterId_ShouldReturnEmpty_WhenNotExists() {
        Optional<ParticipationRequest> found = requestRepository.findByEventIdAndRequesterId(event.getId(), user2.getId());
        assertThat(found).isEmpty();
    }

    @Test
    void countByEventIdAndStatus_ShouldReturnCorrectCount() {
        ParticipationRequest request1 = ParticipationRequest.builder()
                .created(LocalDateTime.now())
                .status(ParticipationStatus.CONFIRMED)
                .event(event)
                .requester(user2)
                .build();

        ParticipationRequest request2 = ParticipationRequest.builder()
                .created(LocalDateTime.now())
                .status(ParticipationStatus.CONFIRMED)
                .event(event)
                .requester(user1)
                .build();

        ParticipationRequest request3 = ParticipationRequest.builder()
                .created(LocalDateTime.now())
                .status(ParticipationStatus.PENDING)
                .event(event)
                .requester(user2)
                .build();

        requestRepository.saveAll(List.of(request1, request2, request3));

        long countConfirmed = requestRepository.countByEventIdAndStatus(event.getId(), ParticipationStatus.CONFIRMED);
        long countPending = requestRepository.countByEventIdAndStatus(event.getId(), ParticipationStatus.PENDING);

        assertThat(countConfirmed).isEqualTo(2);
        assertThat(countPending).isEqualTo(1);
    }
}