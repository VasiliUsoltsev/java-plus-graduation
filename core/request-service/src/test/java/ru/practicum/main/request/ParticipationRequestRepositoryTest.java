package ru.practicum.main.request;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import ru.practicum.main.request.model.ParticipationRequest;
import ru.practicum.main.request.model.enums.ParticipationStatus;
import ru.practicum.main.request.repository.ParticipationRequestRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.config.import=",
        "spring.cloud.config.enabled=false"
})
class ParticipationRequestRepositoryTest {

    @Autowired
    private ParticipationRequestRepository requestRepository;

    private Long eventId;
    private Long user1Id;
    private Long user2Id;

    @BeforeEach
    void setUp() {
        requestRepository.deleteAll();

        eventId = 10L;
        user1Id = 1L;
        user2Id = 2L;
    }

    @Test
    void findAllByRequester_ShouldReturnRequests_WhenExist() {
        ParticipationRequest request1 = ParticipationRequest.builder()
                .created(LocalDateTime.now())
                .status(ParticipationStatus.PENDING)
                .event(eventId)
                .requester(user2Id)
                .build();

        ParticipationRequest request2 = ParticipationRequest.builder()
                .created(LocalDateTime.now())
                .status(ParticipationStatus.CONFIRMED)
                .event(eventId)
                .requester(user2Id)
                .build();

        requestRepository.saveAll(List.of(request1, request2));

        List<ParticipationRequest> found = requestRepository.findAllByRequester(user2Id);

        assertThat(found).hasSize(2);
        assertThat(found).extracting(ParticipationRequest::getRequester)
                .allMatch(id -> id.equals(user2Id));
    }

    @Test
    void findAllByRequester_ShouldReturnEmpty_WhenNoRequests() {
        List<ParticipationRequest> found = requestRepository.findAllByRequester(user2Id);
        assertThat(found).isEmpty();
    }

    @Test
    void findByEventAndRequester_ShouldReturnRequest_WhenExists() {
        ParticipationRequest request = ParticipationRequest.builder()
                .created(LocalDateTime.now())
                .status(ParticipationStatus.PENDING)
                .event(eventId)
                .requester(user2Id)
                .build();

        requestRepository.save(request);

        Optional<ParticipationRequest> found =
                requestRepository.findByEventAndRequester(eventId, user2Id);

        assertThat(found).isPresent();
        assertThat(found.get().getEvent()).isEqualTo(eventId);
        assertThat(found.get().getRequester()).isEqualTo(user2Id);
    }

    @Test
    void findByEventAndRequester_ShouldReturnEmpty_WhenNotExists() {
        Optional<ParticipationRequest> found =
                requestRepository.findByEventAndRequester(eventId, user2Id);
        assertThat(found).isEmpty();
    }

    @Test
    void countByEventAndStatus_ShouldReturnCorrectCount() {
        ParticipationRequest request1 = ParticipationRequest.builder()
                .created(LocalDateTime.now())
                .status(ParticipationStatus.CONFIRMED)
                .event(eventId)
                .requester(user2Id)
                .build();

        ParticipationRequest request2 = ParticipationRequest.builder()
                .created(LocalDateTime.now())
                .status(ParticipationStatus.CONFIRMED)
                .event(eventId)
                .requester(user1Id)
                .build();

        ParticipationRequest request3 = ParticipationRequest.builder()
                .created(LocalDateTime.now())
                .status(ParticipationStatus.PENDING)
                .event(eventId)
                .requester(user2Id)
                .build();

        requestRepository.saveAll(List.of(request1, request2, request3));

        long countConfirmed =
                requestRepository.countByEventAndStatus(eventId, ParticipationStatus.CONFIRMED);
        long countPending =
                requestRepository.countByEventAndStatus(eventId, ParticipationStatus.PENDING);

        assertThat(countConfirmed).isEqualTo(2);
        assertThat(countPending).isEqualTo(1);
    }
}