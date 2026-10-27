package ru.practicum.main.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.main.controller.PrivateEventController;
import ru.practicum.main.dto.LocationDto;
import ru.practicum.main.dto.enums.EventStateAction;
import ru.practicum.main.dto.enums.RequestStatus;
import ru.practicum.main.dto.request.EventRequestStatusUpdateRequest;
import ru.practicum.main.dto.request.NewEventDto;
import ru.practicum.main.dto.request.UpdateEventUserRequest;
import ru.practicum.main.dto.response.*;
import ru.practicum.main.exception.model.NotFoundException;
import ru.practicum.main.model.enums.EventState;
import ru.practicum.main.model.enums.ParticipationStatus;
import ru.practicum.main.service.EventService;

import java.time.LocalDateTime;
import java.time.Month;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PrivateEventController.class)
class PrivateEventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private EventService eventService;

    // ==================== ТЕСТЫ ДЛЯ GET /users/{userId}/events ====================

    @Test
    void getEvents_ShouldReturnEvents_WhenValidParams() throws Exception {
        EventShortDto eventDto = EventShortDto.builder()
                .id(1L)
                .annotation("Test annotation for the event")
                .eventDate(LocalDateTime.of(2026, Month.DECEMBER, 1, 18, 0))
                .title("Test Event")
                .paid(false)
                .category(new CategoryDto(1L, "Концерты"))
                .initiator(new UserShortDto(1L, "John Doe"))
                .confirmedRequests(0L)
                .views(0L)
                .build();

        when(eventService.getUserEvents(eq(1L), eq(0), eq(10)))
                .thenReturn(Collections.singletonList(eventDto));

        mockMvc.perform(get("/users/1/events")
                        .param("from", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].title").value("Test Event"));
    }

    @Test
    void getEvents_ShouldReturnEmptyList_WhenNoEvents() throws Exception {
        when(eventService.getUserEvents(eq(1L), eq(0), eq(10)))
                .thenReturn(List.of());

        mockMvc.perform(get("/users/1/events")
                        .param("from", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void getEvents_ShouldReturnBadRequest_WhenUserIdInvalid() throws Exception {
        mockMvc.perform(get("/users/abc/events"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getEvents_ShouldReturnBadRequest_WhenUserIdNegative() throws Exception {
        mockMvc.perform(get("/users/-1/events"))
                .andExpect(status().isBadRequest());
    }

    // ==================== ТЕСТЫ ДЛЯ POST /users/{userId}/events ====================

    @Test
    void addEvent_ShouldReturnCreated_WhenValidRequest() throws Exception {
        NewEventDto request = NewEventDto.builder()
                .annotation("Test annotation for the event")
                .category(1L)
                .description("Test description for the event which is quite long")
                .eventDate(LocalDateTime.now().plusDays(10))
                .location(new LocationDto(55.75f, 37.62f))
                .paid(false)
                .participantLimit(0)
                .requestModeration(true)
                .title("Test Event Title")
                .build();

        EventFullDto response = EventFullDto.builder()
                .id(1L)
                .annotation("Test annotation for the event")
                .description("Test description for the event which is quite long")
                .eventDate(LocalDateTime.now().plusDays(10))
                .createdOn(LocalDateTime.now())
                .state(EventState.PENDING)
                .title("Test Event Title")
                .paid(false)
                .participantLimit(0)
                .requestModeration(true)
                .category(new CategoryDto(1L, "Концерты"))
                .initiator(new UserShortDto(1L, "John Doe"))
                .confirmedRequests(0L)
                .views(0L)
                .build();

        when(eventService.addEvent(eq(1L), any(NewEventDto.class))).thenReturn(response);

        mockMvc.perform(post("/users/1/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Test Event Title"))
                .andExpect(jsonPath("$.state").value("PENDING"));
    }

    @Test
    void addEvent_ShouldReturnBadRequest_WhenAnnotationIsBlank() throws Exception {
        NewEventDto request = NewEventDto.builder()
                .annotation("")
                .category(1L)
                .description("Test description for the event which is quite long")
                .eventDate(LocalDateTime.now().plusDays(10))
                .location(new LocationDto(55.75f, 37.62f))
                .title("Test Event Title")
                .build();

        mockMvc.perform(post("/users/1/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void addEvent_ShouldReturnBadRequest_WhenAnnotationTooShort() throws Exception {
        NewEventDto request = NewEventDto.builder()
                .annotation("Short")
                .category(1L)
                .description("Test description for the event which is quite long")
                .eventDate(LocalDateTime.now().plusDays(10))
                .location(new LocationDto(55.75f, 37.62f))
                .title("Test Event Title")
                .build();

        mockMvc.perform(post("/users/1/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void addEvent_ShouldReturnBadRequest_WhenTitleIsBlank() throws Exception {
        NewEventDto request = NewEventDto.builder()
                .annotation("Test annotation for the event")
                .category(1L)
                .description("Test description for the event which is quite long")
                .eventDate(LocalDateTime.now().plusDays(10))
                .location(new LocationDto(55.75f, 37.62f))
                .title("")
                .build();

        mockMvc.perform(post("/users/1/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void addEvent_ShouldReturnBadRequest_WhenTitleTooShort() throws Exception {
        NewEventDto request = NewEventDto.builder()
                .annotation("Test annotation for the event")
                .category(1L)
                .description("Test description for the event which is quite long")
                .eventDate(LocalDateTime.now().plusDays(10))
                .location(new LocationDto(55.75f, 37.62f))
                .title("AB")
                .build();

        mockMvc.perform(post("/users/1/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void addEvent_ShouldReturnNotFound_WhenUserNotFound() throws Exception {
        NewEventDto request = NewEventDto.builder()
                .annotation("Test annotation for the event")
                .category(1L)
                .description("Test description for the event which is quite long")
                .eventDate(LocalDateTime.now().plusDays(10))
                .location(new LocationDto(55.75f, 37.62f))
                .title("Test Event Title")
                .build();

        when(eventService.addEvent(eq(999L), any(NewEventDto.class)))
                .thenThrow(new NotFoundException("User with id=999 was not found"));

        mockMvc.perform(post("/users/999/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("The required object was not found."));
    }

    // ==================== ТЕСТЫ ДЛЯ GET /users/{userId}/events/{eventId} ====================

    @Test
    void getEvent_ShouldReturnEvent_WhenExists() throws Exception {
        EventFullDto response = EventFullDto.builder()
                .id(1L)
                .annotation("Test annotation for the event")
                .description("Test description for the event which is quite long")
                .eventDate(LocalDateTime.of(2026, Month.DECEMBER, 1, 18, 0))
                .createdOn(LocalDateTime.of(2026, Month.JANUARY, 1, 10, 0))
                .state(EventState.PENDING)
                .title("Test Event")
                .paid(false)
                .participantLimit(0)
                .requestModeration(true)
                .category(new CategoryDto(1L, "Концерты"))
                .initiator(new UserShortDto(1L, "John Doe"))
                .confirmedRequests(0L)
                .views(0L)
                .build();

        when(eventService.getUserEvent(1L, 1L)).thenReturn(response);

        mockMvc.perform(get("/users/1/events/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Test Event"));
    }

    @Test
    void getEvent_ShouldReturnNotFound_WhenEventDoesNotExist() throws Exception {
        when(eventService.getUserEvent(1L, 999L))
                .thenThrow(new NotFoundException("Event with id=999 was not found"));

        mockMvc.perform(get("/users/1/events/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("The required object was not found."));
    }

    // ==================== ТЕСТЫ ДЛЯ PATCH /users/{userId}/events/{eventId} ====================

    @Test
    void updateEvent_ShouldReturnOk_WhenValidRequest() throws Exception {
        UpdateEventUserRequest request = UpdateEventUserRequest.builder()
                .title("Updated Title")
                .stateAction(EventStateAction.SEND_TO_REVIEW)
                .build();

        EventFullDto response = EventFullDto.builder()
                .id(1L)
                .annotation("Test annotation for the event")
                .description("Test description for the event which is quite long")
                .eventDate(LocalDateTime.of(2026, Month.DECEMBER, 1, 18, 0))
                .createdOn(LocalDateTime.of(2026, Month.JANUARY, 1, 10, 0))
                .state(EventState.PENDING)
                .title("Updated Title")
                .paid(false)
                .participantLimit(0)
                .requestModeration(true)
                .category(new CategoryDto(1L, "Концерты"))
                .initiator(new UserShortDto(1L, "John Doe"))
                .confirmedRequests(0L)
                .views(0L)
                .build();

        when(eventService.updateUserEvent(eq(1L), eq(1L), any(UpdateEventUserRequest.class)))
                .thenReturn(response);

        mockMvc.perform(patch("/users/1/events/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Updated Title"));
    }

    @Test
    void updateEvent_ShouldReturnNotFound_WhenEventDoesNotExist() throws Exception {
        UpdateEventUserRequest request = UpdateEventUserRequest.builder()
                .title("Updated Title")
                .build();

        when(eventService.updateUserEvent(eq(1L), eq(999L), any(UpdateEventUserRequest.class)))
                .thenThrow(new NotFoundException("Event with id=999 was not found"));

        mockMvc.perform(patch("/users/1/events/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("The required object was not found."));
    }

    // ==================== ТЕСТЫ ДЛЯ GET /users/{userId}/events/{eventId}/requests ====================

    @Test
    void getEventRequests_ShouldReturnRequests_WhenExists() throws Exception {
        ParticipationRequestDto requestDto = ParticipationRequestDto.builder()
                .id(1L)
                .created(LocalDateTime.now().minusDays(1))
                .event(1L)
                .requester(2L)
                .status(ParticipationStatus.PENDING)
                .build();

        when(eventService.getEventRequests(1L, 1L))
                .thenReturn(Arrays.asList(requestDto));

        mockMvc.perform(get("/users/1/events/1/requests"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].status").value("PENDING"));
    }

    @Test
    void getEventRequests_ShouldReturnEmptyList_WhenNoRequests() throws Exception {
        when(eventService.getEventRequests(1L, 1L)).thenReturn(List.of());

        mockMvc.perform(get("/users/1/events/1/requests"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    // ==================== ТЕСТЫ ДЛЯ PATCH /users/{userId}/events/{eventId}/requests ====================

    @Test
    void changeRequestStatus_ShouldReturnOk_WhenValidRequest() throws Exception {
        EventRequestStatusUpdateRequest request = EventRequestStatusUpdateRequest.builder()
                .requestIds(Arrays.asList(1L, 2L))
                .status(RequestStatus.CONFIRMED)
                .build();

        EventRequestStatusUpdateResult response = EventRequestStatusUpdateResult.builder()
                .confirmedRequests(Arrays.asList(
                        ParticipationRequestDto.builder().id(1L).status(ParticipationStatus.CONFIRMED).event(1L).requester(2L).build(),
                        ParticipationRequestDto.builder().id(2L).status(ParticipationStatus.CONFIRMED).event(1L).requester(3L).build()
                ))
                .rejectedRequests(List.of())
                .build();

        when(eventService.changeRequestStatus(eq(1L), eq(1L), any(EventRequestStatusUpdateRequest.class)))
                .thenReturn(response);

        mockMvc.perform(patch("/users/1/events/1/requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.confirmedRequests", hasSize(2)))
                .andExpect(jsonPath("$.rejectedRequests", hasSize(0)));
    }

    @Test
    void changeRequestStatus_ShouldReturnNotFound_WhenEventDoesNotExist() throws Exception {
        EventRequestStatusUpdateRequest request = EventRequestStatusUpdateRequest.builder()
                .requestIds(Arrays.asList(1L))
                .status(RequestStatus.CONFIRMED)
                .build();

        when(eventService.changeRequestStatus(eq(1L), eq(999L), any(EventRequestStatusUpdateRequest.class)))
                .thenThrow(new NotFoundException("Event with id=999 was not found"));

        mockMvc.perform(patch("/users/1/events/999/requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void changeRequestStatus_ShouldReturnBadRequest_WhenRequestBodyInvalid() throws Exception {
        mockMvc.perform(patch("/users/1/events/1/requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }
}
