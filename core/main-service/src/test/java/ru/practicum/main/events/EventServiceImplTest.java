package ru.practicum.main.events;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import ru.practicum.main.dto.enums.AdminStateAction;
import ru.practicum.main.dto.enums.EventSort;
import ru.practicum.main.dto.enums.EventStateAction;
import ru.practicum.main.dto.enums.RequestStatus;
import ru.practicum.main.dto.LocationDto;
import ru.practicum.main.dto.request.*;
import ru.practicum.main.dto.response.EventFullDto;
import ru.practicum.main.dto.response.EventRequestStatusUpdateResult;
import ru.practicum.main.dto.response.EventShortDto;
import ru.practicum.main.dto.response.ParticipationRequestDto;
import ru.practicum.main.exception.model.ConflictException;
import ru.practicum.main.exception.model.NotFoundException;
import ru.practicum.main.model.*;
import ru.practicum.main.model.enums.EventState;
import ru.practicum.main.model.enums.ParticipationStatus;
import ru.practicum.main.repository.*;
import ru.practicum.main.service.impl.EventServiceImpl;
import ru.practicum.stat.client.StatClient;
import ru.practicum.stat.dto.ViewStats;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventServiceImplTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private EventSearchRepository eventSearchRepository;

    @Mock
    private ParticipationRequestRepository participationRequestRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private StatClient statClient;

    @Mock
    private EventCommentRepository eventCommentRepository;

    @InjectMocks
    private EventServiceImpl eventService;

    private User user;
    private Category category;
    private Event event;
    private Location location;

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private LocalDateTime baseTime;

    @BeforeEach
    void setUp() {
        baseTime = LocalDateTime.parse("2026-08-27 00:00:00", formatter);
        user = User.builder()
                .id(1L)
                .name("John Doe")
                .email("john@example.com")
                .build();

        category = Category.builder()
                .id(1L)
                .name("Концерты")
                .build();

        location = Location.builder()
                .lat(55.75f)
                .lon(37.62f)
                .build();

        event = Event.builder()
                .id(1L)
                .annotation("Test annotation for the event")
                .description("Test description for the event which is quite long")
                .eventDate(baseTime.plusDays(10))
                .createdOn(baseTime.minusDays(5))
                .paid(false)
                .participantLimit(0)
                .requestModeration(true)
                .title("Test Event Title")
                .state(EventState.PENDING)
                .location(location)
                .initiator(user)
                .category(category)
                .build();
    }

    // ==================== ТЕСТЫ ДЛЯ addEvent ====================

    @Test
    void addEvent_ShouldReturnEventFullDto_WhenValid() {
        NewEventDto newEvent = NewEventDto.builder()
                .annotation("Test annotation for the event")
                .category(1L)
                .description("Test description for the event which is quite long")
                .eventDate(LocalDateTime.now().plusDays(10))
                .location(LocationDto.builder().lat(55.75f).lon(37.62f).build())
                .paid(false)
                .participantLimit(0)
                .requestModeration(true)
                .title("Test Event Title")
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(eventRepository.save(any(Event.class))).thenReturn(event);

        EventFullDto result = eventService.addEvent(1L, newEvent);

        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("Test Event Title");
        assertThat(result.getState()).isEqualTo(EventState.PENDING);

        verify(eventRepository).save(any(Event.class));
    }

    @Test
    void addEvent_ShouldThrowNotFoundException_WhenUserNotFound() {
        NewEventDto newEvent = NewEventDto.builder()
                .annotation("Test annotation for the event")
                .category(1L)
                .description("Test description for the event which is quite long")
                .eventDate(baseTime.plusDays(10))
                .location(LocationDto.builder().lat(55.75f).lon(37.62f).build())
                .title("Test Event Title")
                .build();

        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.addEvent(999L, newEvent))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("User with id=999 was not found");

        verify(eventRepository, never()).save(any());
    }

    @Test
    void addEvent_ShouldThrowNotFoundException_WhenCategoryNotFound() {
        NewEventDto newEvent = NewEventDto.builder()
                .annotation("Test annotation for the event")
                .category(999L)
                .description("Test description for the event which is quite long")
                .eventDate(baseTime.plusDays(10))
                .location(LocationDto.builder().lat(55.75f).lon(37.62f).build())
                .title("Test Event Title")
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.addEvent(1L, newEvent))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Category with id=999 was not found");

        verify(eventRepository, never()).save(any());
    }

    @Test
    void addEvent_ShouldThrowIllegalArgumentException_WhenEventDateInPast() {
        NewEventDto newEvent = NewEventDto.builder()
                .annotation("Test annotation for the event")
                .category(1L)
                .description("Test description for the event which is quite long")
                .eventDate(baseTime.minusHours(1))
                .location(LocationDto.builder().lat(55.75f).lon(37.62f).build())
                .title("Test Event Title")
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

        assertThatThrownBy(() -> eventService.addEvent(1L, newEvent))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("eventDate");

        verify(eventRepository, never()).save(any());
    }

    @Test
    void addEvent_ShouldThrowIllegalArgumentException_WhenEventDateTooSoon() {
        NewEventDto newEvent = NewEventDto.builder()
                .annotation("Test annotation for the event")
                .category(1L)
                .description("Test description for the event which is quite long")
                .eventDate(baseTime.plusHours(1))
                .location(LocationDto.builder().lat(55.75f).lon(37.62f).build())
                .title("Test Event Title")
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

        assertThatThrownBy(() -> eventService.addEvent(1L, newEvent))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("eventDate");

        verify(eventRepository, never()).save(any());
    }

    // ==================== ТЕСТЫ ДЛЯ getUserEvents ====================

    @Test
    void getUserEvents_ShouldReturnEvents_WhenExists() {
        Page<Event> page = new PageImpl<>(List.of(event));

        when(eventRepository.findAllByInitiatorId(eq(1L), any(Pageable.class))).thenReturn(page);
        when(participationRequestRepository.countByEventIdsAndStatus(anyList(), eq(ParticipationStatus.CONFIRMED)))
                .thenReturn(List.<Object[]>of(new Object[]{1L, 0L}));
        when(statClient.getStats(anyString(), anyString(), anyList(), eq(true)))
                .thenReturn(List.of());
        when(eventCommentRepository.countByEventIds(anyList()))
                .thenReturn(List.<Object[]>of(new Object[]{1L, 0L}));

        List<EventShortDto> result = eventService.getUserEvents(1L, 0, 10);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getTitle()).isEqualTo("Test Event Title");
    }

    @Test
    void getUserEvents_ShouldReturnEmptyList_WhenNoEvents() {
        Page<Event> page = Page.empty();

        when(eventRepository.findAllByInitiatorId(eq(1L), any(Pageable.class))).thenReturn(page);

        List<EventShortDto> result = eventService.getUserEvents(1L, 0, 10);

        assertThat(result).isEmpty();
    }

    // ==================== ТЕСТЫ ДЛЯ getUserEvent ====================

    @Test
    void getUserEvent_ShouldReturnEventFullDto_WhenExists() {
        when(eventRepository.findByEventIdAndInitiatorId(1L, 1L)).thenReturn(Optional.of(event));
        when(participationRequestRepository.countByEventIdAndStatus(1L, ParticipationStatus.CONFIRMED)).thenReturn(0L);
        when(statClient.getStats(anyString(), anyString(), anyList(), eq(true))).thenReturn(List.of());

        EventFullDto result = eventService.getUserEvent(1L, 1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getTitle()).isEqualTo("Test Event Title");
    }

    @Test
    void getUserEvent_ShouldThrowNotFoundException_WhenEventNotExists() {
        when(eventRepository.findByEventIdAndInitiatorId(999L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.getUserEvent(1L, 999L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Event with id=999 was not found");
    }

    @Test
    void getUserEvent_ShouldThrowNotFoundException_WhenUserIsNotInitiator() {
        when(eventRepository.findByEventIdAndInitiatorId(1L, 2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.getUserEvent(2L, 1L))
                .isInstanceOf(NotFoundException.class);
    }

    // ==================== ТЕСТЫ ДЛЯ updateUserEvent ====================

    @Test
    void updateUserEvent_ShouldReturnUpdatedEvent_WhenValid() {
        UpdateEventUserRequest update = UpdateEventUserRequest.builder()
                .title("Updated Title")
                .annotation("Updated annotation text for the event")
                .description("Updated description text for the event which is long")
                .build();

        when(eventRepository.findByEventIdAndInitiatorId(1L, 1L)).thenReturn(Optional.of(event));
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(participationRequestRepository.countByEventIdAndStatus(1L, ParticipationStatus.CONFIRMED)).thenReturn(0L);
        when(statClient.getStats(anyString(), anyString(), anyList(), eq(true))).thenReturn(List.of());

        EventFullDto result = eventService.updateUserEvent(1L, 1L, update);

        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("Updated Title");
        assertThat(result.getAnnotation()).isEqualTo("Updated annotation text for the event");
    }

    @Test
    void updateUserEvent_ShouldSetPending_WhenSendToReview() {
        event.setState(EventState.CANCELED);

        UpdateEventUserRequest update = UpdateEventUserRequest.builder()
                .stateAction(EventStateAction.SEND_TO_REVIEW)
                .build();

        when(eventRepository.findByEventIdAndInitiatorId(1L, 1L)).thenReturn(Optional.of(event));
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(participationRequestRepository.countByEventIdAndStatus(1L, ParticipationStatus.CONFIRMED)).thenReturn(0L);
        when(statClient.getStats(anyString(), anyString(), anyList(), eq(true))).thenReturn(List.of());

        EventFullDto result = eventService.updateUserEvent(1L, 1L, update);

        assertThat(result.getState()).isEqualTo(EventState.PENDING);
    }

    @Test
    void updateUserEvent_ShouldSetCanceled_WhenCancelReview() {
        UpdateEventUserRequest update = UpdateEventUserRequest.builder()
                .stateAction(EventStateAction.CANCEL_REVIEW)
                .build();

        when(eventRepository.findByEventIdAndInitiatorId(1L, 1L)).thenReturn(Optional.of(event));
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EventFullDto result = eventService.updateUserEvent(1L, 1L, update);

        assertThat(result.getState()).isEqualTo(EventState.CANCELED);
    }

    @Test
    void updateUserEvent_ShouldThrowConflictException_WhenEventIsPublished() {
        event.setState(EventState.PUBLISHED);

        UpdateEventUserRequest update = UpdateEventUserRequest.builder()
                .title("Updated Title")
                .build();

        when(eventRepository.findByEventIdAndInitiatorId(1L, 1L)).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> eventService.updateUserEvent(1L, 1L, update))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Event must not be published");
    }

    @Test
    void updateUserEvent_ShouldThrowIllegalArgumentException_WhenEventDateTooSoon() {
        UpdateEventUserRequest update = UpdateEventUserRequest.builder()
                .eventDate(baseTime.plusHours(1))
                .build();

        when(eventRepository.findByEventIdAndInitiatorId(1L, 1L)).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> eventService.updateUserEvent(1L, 1L, update))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("eventDate");
    }

    @Test
    void updateUserEvent_ShouldThrowNotFoundException_WhenEventNotExists() {
        UpdateEventUserRequest update = UpdateEventUserRequest.builder().build();

        when(eventRepository.findByEventIdAndInitiatorId(999L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.updateUserEvent(1L, 999L, update))
                .isInstanceOf(NotFoundException.class);
    }

    // ==================== ТЕСТЫ ДЛЯ getEventRequests ====================

    @Test
    void getEventRequests_ShouldReturnRequests_WhenExists() {
        ParticipationRequest request = ParticipationRequest.builder()
                .id(1L)
                .created(baseTime.minusDays(1))
                .status(ParticipationStatus.PENDING)
                .event(event)
                .requester(user)
                .build();

        when(eventRepository.findByEventIdAndInitiatorId(1L, 1L)).thenReturn(Optional.of(event));
        when(participationRequestRepository.findAllByEventId(1L)).thenReturn(List.of(request));

        List<ParticipationRequestDto> result = eventService.getEventRequests(1L, 1L);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getId()).isEqualTo(1L);
        assertThat(result.getFirst().getStatus()).isEqualTo(ParticipationStatus.PENDING);
    }

    @Test
    void getEventRequests_ShouldThrowNotFoundException_WhenEventNotExists() {
        when(eventRepository.findByEventIdAndInitiatorId(999L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.getEventRequests(1L, 999L))
                .isInstanceOf(NotFoundException.class);
    }

    // ==================== ТЕСТЫ ДЛЯ changeRequestStatus ====================

    @Test
    void changeRequestStatus_ShouldRejectAll_WhenRejectAction() {
        ParticipationRequest request = ParticipationRequest.builder()
                .id(1L)
                .status(ParticipationStatus.PENDING)
                .event(event)
                .requester(user)
                .build();

        EventRequestStatusUpdateRequest updateRequest = EventRequestStatusUpdateRequest.builder()
                .requestIds(List.of(1L))
                .status(RequestStatus.REJECTED)
                .build();

        when(eventRepository.findByEventIdAndInitiatorId(1L, 1L)).thenReturn(Optional.of(event));
        when(participationRequestRepository.findAllByIdIn(List.of(1L))).thenReturn(List.of(request));

        EventRequestStatusUpdateResult result = eventService.changeRequestStatus(1L, 1L, updateRequest);

        assertThat(result.getConfirmedRequests()).isEmpty();
        assertThat(result.getRejectedRequests()).hasSize(1);
    }

    @Test
    void changeRequestStatus_ShouldConfirmRequests_WhenConfirmAction() {
        ParticipationRequest request = ParticipationRequest.builder()
                .id(1L)
                .status(ParticipationStatus.PENDING)
                .event(event)
                .requester(user)
                .build();

        ParticipationRequest confirmedRequest = ParticipationRequest.builder()
                .id(1L)
                .status(ParticipationStatus.CONFIRMED)
                .event(event)
                .requester(user)
                .build();

        EventRequestStatusUpdateRequest updateRequest = EventRequestStatusUpdateRequest.builder()
                .requestIds(List.of(1L))
                .status(RequestStatus.CONFIRMED)
                .build();

        when(eventRepository.findByEventIdAndInitiatorId(1L, 1L)).thenReturn(Optional.of(event));
        when(participationRequestRepository.findAllByIdIn(List.of(1L)))
                .thenReturn(List.of(request))
                .thenReturn(List.of(confirmedRequest));
        when(participationRequestRepository.confirmRequestsAtomically(anyList(), eq(1L), eq(0))).thenReturn(1);

        EventRequestStatusUpdateResult result = eventService.changeRequestStatus(1L, 1L, updateRequest);

        assertThat(result.getConfirmedRequests()).hasSize(1);
    }

    @Test
    void changeRequestStatus_ShouldThrowNotFoundException_WhenRequestIdsNotFound() {
        EventRequestStatusUpdateRequest updateRequest = EventRequestStatusUpdateRequest.builder()
                .requestIds(List.of(999L))
                .status(RequestStatus.CONFIRMED)
                .build();

        when(eventRepository.findByEventIdAndInitiatorId(1L, 1L)).thenReturn(Optional.of(event));
        when(participationRequestRepository.findAllByIdIn(List.of(999L))).thenReturn(List.of());

        assertThatThrownBy(() -> eventService.changeRequestStatus(1L, 1L, updateRequest))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("request IDs");
    }

    @Test
    void changeRequestStatus_ShouldThrowConflictException_WhenRequestsNotPending() {
        ParticipationRequest confirmedRequest = ParticipationRequest.builder()
                .id(1L)
                .status(ParticipationStatus.CONFIRMED)
                .event(event)
                .requester(user)
                .build();

        EventRequestStatusUpdateRequest updateRequest = EventRequestStatusUpdateRequest.builder()
                .requestIds(List.of(1L))
                .status(RequestStatus.CONFIRMED)
                .build();

        when(eventRepository.findByEventIdAndInitiatorId(1L, 1L)).thenReturn(Optional.of(event));
        when(participationRequestRepository.findAllByIdIn(List.of(1L))).thenReturn(List.of(confirmedRequest));

        assertThatThrownBy(() -> eventService.changeRequestStatus(1L, 1L, updateRequest))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("PENDING");
    }

    // ==================== ТЕСТЫ ДЛЯ searchEventsAdmin ====================

    @Test
    void searchEventsAdmin_ShouldReturnEvents_WhenFiltersMatch() {
        AdminEventSearchParams params = AdminEventSearchParams.builder()
                .users(List.of(1L))
                .states(List.of("PENDING"))
                .categories(List.of(1L))
                .rangeStart("2020-01-01 00:00:00")
                .rangeEnd("2030-01-01 00:00:00")
                .from(0)
                .size(10)
                .build();

        Page<Event> page = new PageImpl<>(List.of(event));

        when(eventSearchRepository.searchAdmin(any(AdminEventSearchFilter.class),
                any(Pageable.class))).thenReturn(page);
        when(participationRequestRepository.countByEventIdsAndStatus(anyList(), eq(ParticipationStatus.CONFIRMED)))
                .thenReturn(List.<Object[]>of());
        when(statClient.getStats(anyString(), anyString(), anyList(), eq(true))).thenReturn(List.of());

        List<EventFullDto> result = eventService.searchEventsAdmin(params);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getState()).isEqualTo(EventState.PENDING);
    }

    @Test
    void searchEventsAdmin_ShouldReturnEmpty_WhenNoMatch() {
        AdminEventSearchParams params = AdminEventSearchParams.builder()
                .users(List.of(999L))
                .rangeStart("2020-01-01 00:00:00")
                .rangeEnd("2030-01-01 00:00:00")
                .from(0)
                .size(10)
                .build();

        Page<Event> emptyPage = Page.empty();

        when(eventSearchRepository.searchAdmin(any(AdminEventSearchFilter.class),
                any(Pageable.class))).thenReturn(emptyPage);

        List<EventFullDto> result = eventService.searchEventsAdmin(params);

        assertThat(result).isEmpty();
    }

    // ==================== ТЕСТЫ ДЛЯ updateEventAdmin ====================

    @Test
    void updateEventAdmin_ShouldPublishEvent_WhenPending() {
        UpdateEventAdminRequest update = UpdateEventAdminRequest.builder()
                .stateAction(AdminStateAction.PUBLISH_EVENT)
                .build();

        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(participationRequestRepository.countByEventIdAndStatus(1L, ParticipationStatus.CONFIRMED)).thenReturn(0L);
        when(statClient.getStats(anyString(), anyString(), anyList(), eq(true))).thenReturn(List.of());

        EventFullDto result = eventService.updateEventAdmin(1L, update);

        assertThat(result.getState()).isEqualTo(EventState.PUBLISHED);
        assertThat(result.getPublishedOn()).isNotNull();
    }

    @Test
    void updateEventAdmin_ShouldRejectEvent_WhenPending() {
        UpdateEventAdminRequest update = UpdateEventAdminRequest.builder()
                .stateAction(AdminStateAction.REJECT_EVENT)
                .build();

        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EventFullDto result = eventService.updateEventAdmin(1L, update);

        assertThat(result.getState()).isEqualTo(EventState.CANCELED);
    }

    @Test
    void updateEventAdmin_ShouldThrowConflictException_WhenCannotPublish() {
        event.setState(EventState.CANCELED);

        UpdateEventAdminRequest update = UpdateEventAdminRequest.builder()
                .stateAction(AdminStateAction.PUBLISH_EVENT)
                .build();

        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> eventService.updateEventAdmin(1L, update))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Cannot publish");
    }

    @Test
    void updateEventAdmin_ShouldThrowConflictException_WhenCannotReject() {
        event.setState(EventState.PUBLISHED);

        UpdateEventAdminRequest update = UpdateEventAdminRequest.builder()
                .stateAction(AdminStateAction.REJECT_EVENT)
                .build();

        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> eventService.updateEventAdmin(1L, update))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Cannot reject");
    }

    @Test
    void updateEventAdmin_ShouldThrowNotFoundException_WhenEventNotExists() {
        UpdateEventAdminRequest update = UpdateEventAdminRequest.builder().build();

        when(eventRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.updateEventAdmin(999L, update))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Event with id=999 was not found");
    }

    @Test
    void updateEventAdmin_ShouldUpdateFields_WhenProvided() {
        UpdateEventAdminRequest update = UpdateEventAdminRequest.builder()
                .title("Admin Updated Title")
                .annotation("Admin updated annotation for the event")
                .description("Admin updated description for the event which is long")
                .paid(true)
                .participantLimit(100)
                .build();

        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(participationRequestRepository.countByEventIdAndStatus(1L, ParticipationStatus.CONFIRMED)).thenReturn(0L);
        when(statClient.getStats(anyString(), anyString(), anyList(), eq(true))).thenReturn(List.of());

        EventFullDto result = eventService.updateEventAdmin(1L, update);

        assertThat(result.getTitle()).isEqualTo("Admin Updated Title");
        assertThat(result.getPaid()).isTrue();
        assertThat(result.getParticipantLimit()).isEqualTo(100);
    }

    // ==================== ТЕСТЫ ДЛЯ getPublishedEventById ====================

    @Test
    void getPublishedEventById_ShouldReturnEvent_WhenPublished() {
        event.setState(EventState.PUBLISHED);
        event.setPublishedOn(baseTime.minusDays(1));

        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(participationRequestRepository.countByEventIdAndStatus(1L, ParticipationStatus.CONFIRMED)).thenReturn(0L);
        when(statClient.getStats(anyString(), anyString(), anyList(), eq(true))).thenReturn(List.of());

        EventFullDto result = eventService.getPublishedEventById(1L, "192.168.1.1");

        assertThat(result).isNotNull();
        assertThat(result.getState()).isEqualTo(EventState.PUBLISHED);
        verify(statClient).saveHit(anyString(), anyString());
    }

    @Test
    void getPublishedEventById_ShouldThrowNotFoundException_WhenNotPublished() {
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> eventService.getPublishedEventById(1L, "192.168.1.1"))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Event with id=1 was not found");
    }

    @Test
    void getPublishedEventById_ShouldThrowNotFoundException_WhenEventNotExists() {
        when(eventRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.getPublishedEventById(999L, "192.168.1.1"))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Event with id=999 was not found");
    }

    // ==================== ТЕСТЫ ДЛЯ searchPublicEvents ====================

    @Test
    void searchPublicEvents_ShouldReturnEvents_WhenPublishedExist() {
        event.setState(EventState.PUBLISHED);
        event.setPublishedOn(baseTime.minusDays(1));

        PublicEventSearchParams params = PublicEventSearchParams.builder()
                .text("Test")
                .from(0)
                .size(10)
                .build();

        Page<Event> page = new PageImpl<>(List.of(event));

        when(eventSearchRepository.searchPublic(any(PublicEventSearchFilter.class),
                any(Pageable.class))).thenReturn(page);
        when(participationRequestRepository.countByEventIdsAndStatus(anyList(), eq(ParticipationStatus.CONFIRMED)))
                .thenReturn(List.<Object[]>of());
        when(statClient.getStats(anyString(), anyString(), anyList(), eq(true))).thenReturn(List.of());

        List<EventShortDto> result = eventService.searchPublicEvents(params, "192.168.1.1");

        assertThat(result).hasSize(1);
        verify(statClient).saveHit(anyString(), anyString());
    }

    @Test
    void searchPublicEvents_ShouldSortByViews_WhenSortParamIsVIEWS() {
        Event event2 = Event.builder()
                .id(2L)
                .annotation("Another event annotation text here")
                .description("Another event description text here")
                .eventDate(baseTime.plusDays(5))
                .createdOn(baseTime.minusDays(3))
                .paid(true)
                .participantLimit(0)
                .requestModeration(true)
                .title("Another Event")
                .state(EventState.PUBLISHED)
                .location(location)
                .initiator(user)
                .category(category)
                .build();

        PublicEventSearchParams params = PublicEventSearchParams.builder()
                .sort(EventSort.VIEWS)
                .from(0)
                .size(10)
                .build();

        Page<Event> page = new PageImpl<>(List.of(event, event2));

        when(eventSearchRepository.searchPublic(any(PublicEventSearchFilter.class),
                any(Pageable.class))).thenReturn(page);
        when(participationRequestRepository.countByEventIdsAndStatus(anyList(), eq(ParticipationStatus.CONFIRMED)))
                .thenReturn(List.<Object[]>of());
        when(statClient.getStats(anyString(), anyString(), anyList(), eq(true)))
                .thenReturn(List.of(
                        new ViewStats("ewm-main-service", "/events/1", 10L),
                        new ViewStats("ewm-main-service", "/events/2", 50L)
                ));

        List<EventShortDto> result = eventService.searchPublicEvents(params, "192.168.1.1");

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getViews()).isEqualTo(50L);
        assertThat(result.get(1).getViews()).isEqualTo(10L);
    }

    @Test
    void searchPublicEvents_ShouldUseOnlyAvailable_WhenOnlyAvailableTrue() {
        event.setState(EventState.PUBLISHED);
        event.setPublishedOn(baseTime.minusDays(1));

        PublicEventSearchParams params = PublicEventSearchParams.builder()
                .onlyAvailable(true)
                .from(0)
                .size(10)
                .build();

        Page<Event> page = new PageImpl<>(List.of(event));

        when(eventSearchRepository.searchPublic(any(PublicEventSearchFilter.class),
                any(Pageable.class))).thenReturn(page);
        when(participationRequestRepository.countByEventIdsAndStatus(anyList(), eq(ParticipationStatus.CONFIRMED)))
                .thenReturn(List.<Object[]>of());
        when(statClient.getStats(anyString(), anyString(), anyList(), eq(true))).thenReturn(List.of());

        List<EventShortDto> result = eventService.searchPublicEvents(params, "192.168.1.1");

        assertThat(result).hasSize(1);
    }
}
