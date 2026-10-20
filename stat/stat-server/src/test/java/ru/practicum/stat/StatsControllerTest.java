package ru.practicum.stat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.stat.controller.StatsController;
import ru.practicum.stat.dto.EndpointHit;
import ru.practicum.stat.dto.StatsRequest;
import ru.practicum.stat.dto.ViewStats;
import ru.practicum.stat.service.StatsService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StatsController.class)
class StatsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private StatsService statsService;

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Test
    void saveHit_shouldReturnCreatedStatus() throws Exception {
        // Given
        LocalDateTime timestamp = LocalDateTime.parse("2026-08-27 10:00:00", formatter);
        EndpointHit hitDto = EndpointHit.builder()
                .app("test-app")
                .uri("/test/1")
                .ip("192.168.1.1")
                .timestamp(timestamp)
                .build();

        doNothing().when(statsService).saveHit(any(EndpointHit.class));


        mockMvc.perform(post("/hit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(hitDto)))
                .andExpect(status().isCreated());

        verify(statsService, times(1)).saveHit(any(EndpointHit.class));
    }

    @Test
    void getStats_withAllParams_shouldReturnStats() throws Exception {

        LocalDateTime start = LocalDateTime.parse("2026-08-27 00:00:00", formatter);
        LocalDateTime end = LocalDateTime.parse("2026-08-27 02:00:00", formatter);

        List<ViewStats> expectedStats = List.of(
                new ViewStats("test-app", "/test/1", 2L),
                new ViewStats("another-app", "/test/2", 1L)
        );

        when(statsService.getStats(any(StatsRequest.class))).thenReturn(expectedStats);


        mockMvc.perform(get("/stats")
                        .param("start", start.format(formatter))
                        .param("end", end.format(formatter))
                        .param("uris", "/test/1", "/test/2")
                        .param("unique", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].app").value("test-app"))
                .andExpect(jsonPath("$[0].uri").value("/test/1"))
                .andExpect(jsonPath("$[0].hits").value(2))
                .andExpect(jsonPath("$[1].app").value("another-app"))
                .andExpect(jsonPath("$[1].uri").value("/test/2"))
                .andExpect(jsonPath("$[1].hits").value(1));

        verify(statsService, times(1)).getStats(any(StatsRequest.class));
    }

    @Test
    void getStats_withoutUris_shouldReturnStats() throws Exception {

        LocalDateTime start = LocalDateTime.parse("2026-08-27 09:00:00", formatter);
        LocalDateTime end = LocalDateTime.parse("2026-08-27 11:00:00", formatter);

        List<ViewStats> expectedStats = List.of(
                new ViewStats("test-app", "/test/1", 3L),
                new ViewStats("another-app", "/test/2", 1L)
        );

        when(statsService.getStats(any(StatsRequest.class)))
                .thenReturn(expectedStats);


        mockMvc.perform(get("/stats")
                        .param("start", start.format(formatter))
                        .param("end", end.format(formatter))
                        .param("unique", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].app").value("test-app"))
                .andExpect(jsonPath("$[0].uri").value("/test/1"))
                .andExpect(jsonPath("$[0].hits").value(3));

        verify(statsService, times(1)).getStats(any(StatsRequest.class));
    }

    @Test
    void getStats_withInvalidDateFormat_shouldReturnBadRequest() throws Exception {

        String invalidDate = "2026-08-27 10:00";


        mockMvc.perform(get("/stats")
                        .param("start", invalidDate)
                        .param("end", invalidDate))
                .andExpect(status().isBadRequest());
    }

    @Test
    void saveHit_withInvalidData_shouldReturnBadRequest() throws Exception {

        LocalDateTime fixedDate = LocalDateTime.parse("2026-08-27 10:00:00", formatter);

        EndpointHit invalidHit = EndpointHit.builder()
                .app("") // Пустое поле должно вызвать ошибку валидации
                .uri("/test/1")
                .ip("192.168.1.1")
                .timestamp(fixedDate)
                .build();


        mockMvc.perform(post("/hit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidHit)))
                .andExpect(status().isBadRequest());
    }
}