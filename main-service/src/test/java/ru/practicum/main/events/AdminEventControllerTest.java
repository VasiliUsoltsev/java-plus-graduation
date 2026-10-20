package ru.practicum.main.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.main.controller.AdminEventController;
import ru.practicum.main.dto.enums.AdminStateAction;
import ru.practicum.main.dto.request.AdminEventSearchParams;
import ru.practicum.main.dto.request.UpdateEventAdminRequest;
import ru.practicum.main.dto.response.CategoryDto;
import ru.practicum.main.dto.response.EventFullDto;
import ru.practicum.main.dto.response.UserShortDto;
import ru.practicum.main.exception.model.ConflictException;
import ru.practicum.main.exception.model.NotFoundException;
import ru.practicum.main.model.enums.EventState;
import ru.practicum.main.service.EventService;

import java.time.LocalDateTime;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminEventController.class)
class AdminEventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private EventService eventService;

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private LocalDateTime baseTime;

    @BeforeEach
    void setUp() {
        baseTime = LocalDateTime.parse("2026-08-27 00:00:00", formatter);
    }

    // ==================== ТЕСТЫ ДЛЯ GET /admin/events ====================

    @Test
    void searchEvents_ShouldReturnEvents_WhenValidParams() throws Exception {
        EventFullDto eventDto = EventFullDto.builder()
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

        when(eventService.searchEventsAdmin(any(AdminEventSearchParams.class)))
                .thenReturn(Arrays.asList(eventDto));

        mockMvc.perform(get("/admin/events")
                        .param("from", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].title").value("Test Event"))
                .andExpect(jsonPath("$[0].state").value("PENDING"));
    }

    @Test
    void searchEvents_ShouldReturnEmptyList_WhenNoEvents() throws Exception {
        when(eventService.searchEventsAdmin(any(AdminEventSearchParams.class)))
                .thenReturn(List.of());

        mockMvc.perform(get("/admin/events")
                        .param("from", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void searchEvents_ShouldUseDefaultValues_WhenParamsNotProvided() throws Exception {
        when(eventService.searchEventsAdmin(any(AdminEventSearchParams.class)))
                .thenReturn(List.of());

        mockMvc.perform(get("/admin/events"))
                .andExpect(status().isOk());
    }

    @Test
    void searchEvents_ShouldReturnBadRequest_WhenSizeNegative() throws Exception {
        mockMvc.perform(get("/admin/events")
                        .param("from", "0")
                        .param("size", "-5"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void searchEvents_ShouldReturnBadRequest_WhenFromNegative() throws Exception {
        mockMvc.perform(get("/admin/events")
                        .param("from", "-1")
                        .param("size", "10"))
                .andExpect(status().isBadRequest());
    }

    // ==================== ТЕСТЫ ДЛЯ PATCH /admin/events/{eventId} ====================

    @Test
    void updateEvent_ShouldReturnOk_WhenValidRequest() throws Exception {
        UpdateEventAdminRequest request = UpdateEventAdminRequest.builder()
                .stateAction(AdminStateAction.PUBLISH_EVENT)
                .build();

        EventFullDto response = EventFullDto.builder()
                .id(1L)
                .annotation("Test annotation for the event")
                .description("Test description for the event which is quite long")
                .eventDate(LocalDateTime.of(2026, Month.DECEMBER, 1, 18, 0))
                .createdOn(LocalDateTime.of(2026, Month.JANUARY, 1, 10, 0))
                .publishedOn(baseTime)
                .state(EventState.PUBLISHED)
                .title("Test Event")
                .paid(false)
                .participantLimit(0)
                .requestModeration(true)
                .category(new CategoryDto(1L, "Концерты"))
                .initiator(new UserShortDto(1L, "John Doe"))
                .confirmedRequests(0L)
                .views(0L)
                .build();

        when(eventService.updateEventAdmin(eq(1L), any(UpdateEventAdminRequest.class)))
                .thenReturn(response);

        mockMvc.perform(patch("/admin/events/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.state").value("PUBLISHED"))
                .andExpect(jsonPath("$.title").value("Test Event"));
    }

    @Test
    void updateEvent_ShouldReturnNotFound_WhenEventDoesNotExist() throws Exception {
        UpdateEventAdminRequest request = UpdateEventAdminRequest.builder()
                .stateAction(AdminStateAction.PUBLISH_EVENT)
                .build();

        when(eventService.updateEventAdmin(eq(999L), any(UpdateEventAdminRequest.class)))
                .thenThrow(new NotFoundException("Event with id=999 was not found"));

        mockMvc.perform(patch("/admin/events/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("The required object was not found."))
                .andExpect(jsonPath("$.message").value("Event with id=999 was not found"));
    }

    @Test
    void updateEvent_ShouldReturnConflict_WhenCannotPublish() throws Exception {
        UpdateEventAdminRequest request = UpdateEventAdminRequest.builder()
                .stateAction(AdminStateAction.PUBLISH_EVENT)
                .build();

        when(eventService.updateEventAdmin(eq(1L), any(UpdateEventAdminRequest.class)))
                .thenThrow(new ConflictException("Cannot publish the event because it's not in the right state"));

        mockMvc.perform(patch("/admin/events/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("For the requested operation the conditions are not met."));
    }

    @Test
    void updateEvent_ShouldReturnBadRequest_WhenEventIdNegative() throws Exception {
        UpdateEventAdminRequest request = UpdateEventAdminRequest.builder()
                .stateAction(AdminStateAction.PUBLISH_EVENT)
                .build();

        mockMvc.perform(patch("/admin/events/-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateEvent_ShouldReturnBadRequest_WhenEventIdIsNotNumber() throws Exception {
        UpdateEventAdminRequest request = UpdateEventAdminRequest.builder()
                .stateAction(AdminStateAction.PUBLISH_EVENT)
                .build();

        mockMvc.perform(patch("/admin/events/abc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
