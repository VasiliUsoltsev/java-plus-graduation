package ru.practicum.main.requests;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.main.dto.response.ParticipationRequestDto;
import ru.practicum.main.exception.model.ConflictException;
import ru.practicum.main.exception.model.NotFoundException;
import ru.practicum.main.model.Category;
import ru.practicum.main.model.Event;
import ru.practicum.main.model.ParticipationRequest;
import ru.practicum.main.model.User;
import ru.practicum.main.model.enums.EventState;
import ru.practicum.main.model.enums.ParticipationStatus;
import ru.practicum.main.repository.EventRepository;
import ru.practicum.main.repository.ParticipationRequestRepository;
import ru.practicum.main.repository.UserRepository;
import ru.practicum.main.service.impl.ParticipationRequestServiceImpl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ParticipationRequestServiceImplTest {

    @Mock
    private ParticipationRequestRepository requestRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ParticipationRequestServiceImpl requestService;

    private User user;
    private User anotherUser;
    private Category category;
    private Event event;
    private ParticipationRequest request;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .name("John Doe")
                .email("john@example.com")
                .build();

        anotherUser = User.builder()
                .id(2L)
                .name("Jane Smith")
                .email("jane@example.com")
                .build();

        category = Category.builder()
                .id(1L)
                .name("Концерты")
                .build();

        event = Event.builder()
                .id(10L)
                .annotation("Test event")
                .description("Test description")
                .title("Test Event")
                .state(EventState.PUBLISHED)
                .initiator(user) // user is initiator
                .participantLimit(0)
                .requestModeration(true)
                .category(category)
                .eventDate(LocalDateTime.now().plusDays(5))
                .createdOn(LocalDateTime.now())
                .paid(false)
                .build();

        request = ParticipationRequest.builder()
                .id(1L)
                .created(LocalDateTime.now())
                .status(ParticipationStatus.PENDING)
                .event(event)
                .requester(anotherUser)
                .build();
    }


    @Test
    void getUserRequests_ShouldReturnList_WhenUserExists() {
        when(userRepository.existsById(1L)).thenReturn(true);
        when(requestRepository.findAllByRequesterId(1L)).thenReturn(List.of(request));

        List<ParticipationRequestDto> result = requestService.getUserRequests(1L);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getId()).isEqualTo(1L);
        assertThat(result.getFirst().getStatus()).isEqualTo(ParticipationStatus.PENDING);
    }

    @Test
    void getUserRequests_ShouldThrowNotFoundException_WhenUserNotFound() {
        when(userRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> requestService.getUserRequests(999L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("User with id=999 not found");
    }

    @Test
    void createRequest_ShouldReturnPending_WhenModerationEnabledAndLimitNotZero() {
        event.setRequestModeration(true);
        event.setParticipantLimit(5);
        when(userRepository.findById(2L)).thenReturn(Optional.of(anotherUser));
        when(eventRepository.findById(10L)).thenReturn(Optional.of(event));
        when(requestRepository.findByEventIdAndRequesterId(10L, 2L)).thenReturn(Optional.empty());
        when(requestRepository.countByEventIdAndStatus(10L, ParticipationStatus.CONFIRMED)).thenReturn(0L);
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
        event.setRequestModeration(false);
        when(userRepository.findById(2L)).thenReturn(Optional.of(anotherUser));
        when(eventRepository.findById(10L)).thenReturn(Optional.of(event));
        when(requestRepository.findByEventIdAndRequesterId(10L, 2L)).thenReturn(Optional.empty());
        when(requestRepository.countByEventIdAndStatus(10L, ParticipationStatus.CONFIRMED)).thenReturn(0L);
        when(requestRepository.save(any(ParticipationRequest.class))).thenAnswer(inv -> {
            ParticipationRequest saved = inv.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        ParticipationRequestDto result = requestService.createRequest(2L, 10L);

        assertThat(result.getStatus()).isEqualTo(ParticipationStatus.CONFIRMED);
    }

    @Test
    void createRequest_ShouldThrowNotFoundException_WhenUserNotFound() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> requestService.createRequest(999L, 10L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("User with id=999 not found");
    }

    @Test
    void createRequest_ShouldThrowNotFoundException_WhenEventNotFound() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(anotherUser));
        when(eventRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> requestService.createRequest(2L, 999L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Event with id=999 not found");
    }

    @Test
    void createRequest_ShouldThrowConflictException_WhenEventNotPublished() {
        event.setState(EventState.PENDING);
        when(userRepository.findById(2L)).thenReturn(Optional.of(anotherUser));
        when(eventRepository.findById(10L)).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> requestService.createRequest(2L, 10L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Event is not published");
    }

    @Test
    void createRequest_ShouldThrowConflictException_WhenInitiatorTriesToParticipate() {
        // user is initiator (id=1), try to create request by user=1
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(eventRepository.findById(10L)).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> requestService.createRequest(1L, 10L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Initiator cannot participate in their own event");
    }

    @Test
    void createRequest_ShouldThrowConflictException_WhenDuplicateRequest() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(anotherUser));
        when(eventRepository.findById(10L)).thenReturn(Optional.of(event));
        when(requestRepository.findByEventIdAndRequesterId(10L, 2L)).thenReturn(Optional.of(request));

        assertThatThrownBy(() -> requestService.createRequest(2L, 10L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Duplicate request for this event");
    }

    @Test
    void createRequest_ShouldThrowConflictException_WhenParticipantLimitReached() {
        event.setParticipantLimit(1);
        when(userRepository.findById(2L)).thenReturn(Optional.of(anotherUser));
        when(eventRepository.findById(10L)).thenReturn(Optional.of(event));
        when(requestRepository.findByEventIdAndRequesterId(10L, 2L)).thenReturn(Optional.empty());
        when(requestRepository.countByEventIdAndStatus(10L, ParticipationStatus.CONFIRMED)).thenReturn(1L);

        assertThatThrownBy(() -> requestService.createRequest(2L, 10L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Participant limit has been reached");
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
        // request belongs to anotherUser (id=2), but we try to cancel by user=3
        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));

        assertThatThrownBy(() -> requestService.cancelRequest(3L, 1L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Request with id=1 not found for this user");
    }
}