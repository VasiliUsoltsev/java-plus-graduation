package ru.practicum.main.events;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.main.controller.PublicEventController;
import ru.practicum.main.dto.response.CategoryDto;
import ru.practicum.main.dto.response.EventFullDto;
import ru.practicum.main.dto.response.EventShortDto;
import ru.practicum.main.dto.response.UserShortDto;
import ru.practicum.main.exception.model.NotFoundException;
import ru.practicum.main.model.enums.EventState;
import ru.practicum.main.service.EventService;

import java.time.LocalDateTime;
import java.time.Month;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PublicEventController.class)
class PublicEventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EventService eventService;

    // ==================== ТЕСТЫ ДЛЯ GET /events (поиск) ====================

    @Test
    void searchEvents_ShouldReturnEvents_WhenValidParams() throws Exception {
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

        when(eventService.searchPublicEvents(any(), anyString()))
                .thenReturn(Collections.singletonList(eventDto));

        mockMvc.perform(get("/events")
                        .param("text", "Test")
                        .param("categories", "1")
                        .param("paid", "false")
                        .param("sort", "EVENT_DATE")
                        .param("from", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].title").value("Test Event"));
    }

    @Test
    void searchEvents_ShouldReturnEmptyList_WhenNoResults() throws Exception {
        when(eventService.searchPublicEvents(any(), anyString()))
                .thenReturn(List.of());

        mockMvc.perform(get("/events")
                        .param("text", "nonexistent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void searchEvents_ShouldUseDefaultValues_WhenParamsNotProvided() throws Exception {
        when(eventService.searchPublicEvents(any(), anyString()))
                .thenReturn(List.of());

        mockMvc.perform(get("/events"))
                .andExpect(status().isOk());
    }

    @Test
    void searchEvents_ShouldReturnBadRequest_WhenSizeNegative() throws Exception {
        mockMvc.perform(get("/events")
                        .param("size", "-5"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void searchEvents_ShouldReturnBadRequest_WhenFromNegative() throws Exception {
        mockMvc.perform(get("/events")
                        .param("from", "-1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void searchEvents_ShouldReturnBadRequest_WhenSortInvalid() throws Exception {
        mockMvc.perform(get("/events")
                        .param("sort", "INVALID"))
                .andExpect(status().isBadRequest());
    }

    // ==================== ТЕСТЫ ДЛЯ GET /events/{id} ====================

    @Test
    void getEvent_ShouldReturnEvent_WhenPublished() throws Exception {
        EventFullDto response = EventFullDto.builder()
                .id(1L)
                .annotation("Test annotation for the event")
                .description("Test description for the event which is quite long")
                .eventDate(LocalDateTime.of(2026, Month.DECEMBER, 1, 18, 0))
                .createdOn(LocalDateTime.of(2026, Month.JANUARY, 1, 10, 0))
                .publishedOn(LocalDateTime.now())
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

        when(eventService.getPublishedEventById(1L, "127.0.0.1")).thenReturn(response);

        mockMvc.perform(get("/events/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Test Event"))
                .andExpect(jsonPath("$.state").value("PUBLISHED"));
    }

    @Test
    void getEvent_ShouldReturnNotFound_WhenEventNotPublished() throws Exception {
        when(eventService.getPublishedEventById(1L, "127.0.0.1"))
                .thenThrow(new NotFoundException("Event with id=1 was not found"));

        mockMvc.perform(get("/events/1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("The required object was not found."))
                .andExpect(jsonPath("$.message").value("Event with id=1 was not found"));
    }

    @Test
    void getEvent_ShouldReturnNotFound_WhenEventDoesNotExist() throws Exception {
        when(eventService.getPublishedEventById(999L, "127.0.0.1"))
                .thenThrow(new NotFoundException("Event with id=999 was not found"));

        mockMvc.perform(get("/events/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Event with id=999 was not found"));
    }

    @Test
    void getEvent_ShouldReturnBadRequest_WhenIdIsNotNumber() throws Exception {
        mockMvc.perform(get("/events/abc"))
                .andExpect(status().isBadRequest());
    }
}
