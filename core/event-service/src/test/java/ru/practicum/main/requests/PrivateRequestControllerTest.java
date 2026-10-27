package ru.practicum.main.requests;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.main.controller.PrivateRequestController;
import ru.practicum.main.dto.response.ParticipationRequestDto;
import ru.practicum.main.exception.model.ConflictException;
import ru.practicum.main.exception.model.NotFoundException;
import ru.practicum.main.model.enums.ParticipationStatus;
import ru.practicum.main.service.ParticipationRequestService;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PrivateRequestController.class)
class PrivateRequestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ParticipationRequestService requestService;

    private ParticipationRequestDto createRequestDto(Long id, Long eventId, Long requesterId, ParticipationStatus status) {
        return ParticipationRequestDto.builder()
                .id(id)
                .created(LocalDateTime.now())
                .event(eventId)
                .requester(requesterId)
                .status(status)
                .build();
    }

    @Test
    void getUserRequests_ShouldReturnList_WhenExists() throws Exception {
        List<ParticipationRequestDto> requests = List.of(
                createRequestDto(1L, 10L, 1L, ParticipationStatus.PENDING),
                createRequestDto(2L, 20L, 1L, ParticipationStatus.CONFIRMED)
        );

        when(requestService.getUserRequests(1L)).thenReturn(requests);

        mockMvc.perform(get("/users/1/requests"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].status").value("PENDING"))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].status").value("CONFIRMED"));
    }

    @Test
    void getUserRequests_ShouldReturnEmptyList_WhenNoRequests() throws Exception {
        when(requestService.getUserRequests(1L)).thenReturn(List.of());

        mockMvc.perform(get("/users/1/requests"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void getUserRequests_ShouldReturnNotFound_WhenUserDoesNotExist() throws Exception {
        when(requestService.getUserRequests(999L))
                .thenThrow(new NotFoundException("User with id=999 not found"));

        mockMvc.perform(get("/users/999/requests"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("The required object was not found."))
                .andExpect(jsonPath("$.message").value("User with id=999 not found"));
    }

    @Test
    void getUserRequests_ShouldReturnBadRequest_WhenUserIdInvalid() throws Exception {
        mockMvc.perform(get("/users/abc/requests"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getUserRequests_ShouldReturnBadRequest_WhenUserIdNegative() throws Exception {
        mockMvc.perform(get("/users/-1/requests"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createRequest_ShouldReturnCreated_WhenValid() throws Exception {
        ParticipationRequestDto response = createRequestDto(1L, 10L, 2L, ParticipationStatus.PENDING);

        when(requestService.createRequest(2L, 10L)).thenReturn(response);

        mockMvc.perform(post("/users/2/requests")
                        .param("eventId", "10"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.event").value(10))
                .andExpect(jsonPath("$.requester").value(2))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void createRequest_ShouldReturnBadRequest_WhenEventIdMissing() throws Exception {
        mockMvc.perform(post("/users/2/requests"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createRequest_ShouldReturnBadRequest_WhenEventIdIsNotNumber() throws Exception {
        mockMvc.perform(post("/users/2/requests")
                        .param("eventId", "abc"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createRequest_ShouldReturnBadRequest_WhenEventIdNegative() throws Exception {
        mockMvc.perform(post("/users/2/requests")
                        .param("eventId", "-1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createRequest_ShouldReturnNotFound_WhenEventNotExists() throws Exception {
        when(requestService.createRequest(2L, 999L))
                .thenThrow(new NotFoundException("Event with id=999 not found"));

        mockMvc.perform(post("/users/2/requests")
                        .param("eventId", "999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("The required object was not found."));
    }

    @Test
    void createRequest_ShouldReturnConflict_WhenBusinessRuleViolated() throws Exception {
        when(requestService.createRequest(2L, 10L))
                .thenThrow(new ConflictException("Duplicate request for this event"));

        mockMvc.perform(post("/users/2/requests")
                        .param("eventId", "10"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("For the requested operation the conditions are not met."))
                .andExpect(jsonPath("$.message").value("Duplicate request for this event"));
    }

    @Test
    void cancelRequest_ShouldReturnOk_WhenValid() throws Exception {
        ParticipationRequestDto response = createRequestDto(1L, 10L, 2L, ParticipationStatus.CANCELED);

        when(requestService.cancelRequest(2L, 1L)).thenReturn(response);

        mockMvc.perform(patch("/users/2/requests/1/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("CANCELED"));
    }

    @Test
    void cancelRequest_ShouldReturnNotFound_WhenRequestNotExists() throws Exception {
        when(requestService.cancelRequest(2L, 999L))
                .thenThrow(new NotFoundException("Request with id=999 not found"));

        mockMvc.perform(patch("/users/2/requests/999/cancel"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("The required object was not found."));
    }

    @Test
    void cancelRequest_ShouldReturnNotFound_WhenRequestBelongsToAnotherUser() throws Exception {
        when(requestService.cancelRequest(2L, 1L))
                .thenThrow(new NotFoundException("Request with id=1 not found for this user"));

        mockMvc.perform(patch("/users/2/requests/1/cancel"))
                .andExpect(status().isNotFound());
    }

    @Test
    void cancelRequest_ShouldReturnBadRequest_WhenUserIdInvalid() throws Exception {
        mockMvc.perform(patch("/users/abc/requests/1/cancel"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void cancelRequest_ShouldReturnBadRequest_WhenRequestIdInvalid() throws Exception {
        mockMvc.perform(patch("/users/1/requests/abc/cancel"))
                .andExpect(status().isBadRequest());
    }
}