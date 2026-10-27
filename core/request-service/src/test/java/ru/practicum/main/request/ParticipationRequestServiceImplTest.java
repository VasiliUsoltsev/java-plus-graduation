package ru.practicum.main.request;

import feign.FeignException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.main.request.client.EventClient;
import ru.practicum.main.request.client.UserClient;
import ru.practicum.main.request.dto.client.EventDto;
import ru.practicum.main.request.dto.client.EventState;
import ru.practicum.main.request.dto.response.ParticipationRequestDto;
import ru.practicum.main.request.exception.model.ConflictException;
import ru.practicum.main.request.exception.model.EventServiceUnavailableException;
import ru.practicum.main.request.exception.model.NotFoundException;
import ru.practicum.main.request.exception.model.UserServiceUnavailableException;
import ru.practicum.main.request.model.ParticipationRequest;
import ru.practicum.main.request.model.enums.ParticipationStatus;
import ru.practicum.main.request.repository.ParticipationRequestRepository;
import ru.practicum.main.request.service.impl.ParticipationRequestServiceImpl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ParticipationRequestServiceImplTest {

    @Mock
    private ParticipationRequestRepository requestRepository;

    @Mock
    private EventClient eventClient;

    @Mock
    private UserClient userClient;

    @InjectMocks
    private ParticipationRequestServiceImpl requestService;

    private EventDto event;
    private ParticipationRequest request;

    @BeforeEach
    void setUp() {
        event = new EventDto(
                10L,
                "Test event",
                "Test description",
                LocalDateTime.now().plusDays(5),
                LocalDateTime.now(),
                null,
                false,
                0,
                true,
                "Test Event",
                EventState.PUBLISHED,
                null,
                1L,
                1L
        );

        request = ParticipationRequest.builder()
                .id(1L)
                .created(LocalDateTime.now())
                .status(ParticipationStatus.PENDING)
                .event(10L)
                .requester(2L)
                .build();
    }

    @Test
    void getUserRequests_ShouldReturnList_WhenUserExists() {
        when(userClient.existsById(1L)).thenReturn(true);
        when(requestRepository.findAllByRequester(1L)).thenReturn(List.of(request));

        List<ParticipationRequestDto> result = requestService.getUserRequests(1L);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getId()).isEqualTo(1L);
        assertThat(result.getFirst().getStatus()).isEqualTo(ParticipationStatus.PENDING);
    }

    @Test
    void getUserRequests_ShouldThrowNotFoundException_WhenUserNotFound() {
        when(userClient.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> requestService.getUserRequests(999L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("User with id=999 not found");
    }

    @Test
    void getUserRequests_ShouldThrowUserServiceUnavailableException_WhenUserServiceUnavailable() {
        FeignException unavailable = mock(FeignException.class);
        when(userClient.existsById(anyLong())).thenThrow(unavailable);

        assertThatThrownBy(() -> requestService.getUserRequests(1L))
                .isInstanceOf(UserServiceUnavailableException.class);
    }

    @Test
    void createRequest_ShouldReturnPending_WhenModerationEnabledAndLimitNotZero() {
        event = new EventDto(
                10L, "Test event", "Test description",
                LocalDateTime.now().plusDays(5), LocalDateTime.now(), null,
                false, 5, true, "Test Event", EventState.PUBLISHED,
                null, 1L, 1L
        );
        when(eventClient.findById(10L)).thenReturn(event);
        when(requestRepository.findByEventAndRequester(10L, 2L)).thenReturn(Optional.empty());
        when(requestRepository.countByEventAndStatus(10L, ParticipationStatus.CONFIRMED)).thenReturn(0L);
        when(requestRepository.save(any(ParticipationRequest.class))).thenAnswer(inv -> {
            ParticipationRequest saved = inv.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        ParticipationRequestDto result = requestService.createRequest(2L, 10L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getStatus()).isEqualTo(ParticipationStatus.PENDING);
        verify(requestRepository).save(any(ParticipationRequest.class));
    }

    @Test
    void createRequest_ShouldReturnConfirmed_WhenModerationDisabled() {
        event = new EventDto(
                10L, "Test event", "Test description",
                LocalDateTime.now().plusDays(5), LocalDateTime.now(), null,
                false, 0, false, "Test Event", EventState.PUBLISHED,
                null, 1L, 1L
        );
        when(eventClient.findById(10L)).thenReturn(event);
        when(requestRepository.findByEventAndRequester(10L, 2L)).thenReturn(Optional.empty());
        when(requestRepository.countByEventAndStatus(10L, ParticipationStatus.CONFIRMED)).thenReturn(0L);
        when(requestRepository.save(any(ParticipationRequest.class))).thenAnswer(inv -> {
            ParticipationRequest saved = inv.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        ParticipationRequestDto result = requestService.createRequest(2L, 10L);

        assertThat(result.getStatus()).isEqualTo(ParticipationStatus.CONFIRMED);
    }

    @Test
    void createRequest_ShouldThrowNotFoundException_WhenEventNotFound() {
        FeignException notFound = mock(FeignException.NotFound.class);
        when(eventClient.findById(999L)).thenThrow(notFound);

        assertThatThrownBy(() -> requestService.createRequest(2L, 999L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Event with id=999 not found");
    }

    @Test
    void createRequest_ShouldThrowConflictException_WhenEventNotPublished() {
        event = new EventDto(
                10L, "Test event", "Test description",
                LocalDateTime.now().plusDays(5), LocalDateTime.now(), null,
                false, 0, true, "Test Event", EventState.PENDING,
                null, 1L, 1L
        );
        when(eventClient.findById(10L)).thenReturn(event);

        assertThatThrownBy(() -> requestService.createRequest(2L, 10L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Event is not published");
    }

    @Test
    void createRequest_ShouldThrowConflictException_WhenInitiatorTriesToParticipate() {
        when(eventClient.findById(10L)).thenReturn(event);

        assertThatThrownBy(() -> requestService.createRequest(1L, 10L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Initiator cannot participate in their own event");
    }

    @Test
    void createRequest_ShouldThrowConflictException_WhenDuplicateRequest() {
        when(eventClient.findById(10L)).thenReturn(event);
        when(requestRepository.findByEventAndRequester(10L, 2L)).thenReturn(Optional.of(request));

        assertThatThrownBy(() -> requestService.createRequest(2L, 10L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Duplicate request for this event");
    }

    @Test
    void createRequest_ShouldThrowConflictException_WhenParticipantLimitReached() {
        event = new EventDto(
                10L, "Test event", "Test description",
                LocalDateTime.now().plusDays(5), LocalDateTime.now(), null,
                false, 1, true, "Test Event", EventState.PUBLISHED,
                null, 1L, 1L
        );
        when(eventClient.findById(10L)).thenReturn(event);
        when(requestRepository.findByEventAndRequester(10L, 2L)).thenReturn(Optional.empty());
        when(requestRepository.countByEventAndStatus(10L, ParticipationStatus.CONFIRMED)).thenReturn(1L);

        assertThatThrownBy(() -> requestService.createRequest(2L, 10L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Participant limit has been reached");
    }

    @Test
    void createRequest_ShouldThrowEventServiceUnavailableException_WhenEventServiceUnavailable() {
        FeignException unavailable = mock(FeignException.class);
        when(eventClient.findById(anyLong())).thenThrow(unavailable);

        assertThatThrownBy(() -> requestService.createRequest(2L, 10L))
                .isInstanceOf(EventServiceUnavailableException.class);
    }

    @Test
    void cancelRequest_ShouldReturnCanceled_WhenValid() {
        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(requestRepository.save(any(ParticipationRequest.class))).thenReturn(request);

        ParticipationRequestDto result = requestService.cancelRequest(2L, 1L);

        assertThat(result.getStatus()).isEqualTo(ParticipationStatus.CANCELED);
        verify(requestRepository).save(request);
    }

    @Test
    void cancelRequest_ShouldThrowNotFoundException_WhenRequestNotFound() {
        when(requestRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> requestService.cancelRequest(2L, 999L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Request with id=999 not found");
    }

    @Test
    void cancelRequest_ShouldThrowNotFoundException_WhenRequestBelongsToAnotherUser() {
        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));

        assertThatThrownBy(() -> requestService.cancelRequest(3L, 1L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Request with id=1 not found for this user");
    }
}