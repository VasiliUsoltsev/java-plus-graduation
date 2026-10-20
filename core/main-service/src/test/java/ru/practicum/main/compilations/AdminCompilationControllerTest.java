package ru.practicum.main.compilations;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.main.controller.AdminCompilationController;
import ru.practicum.main.dto.request.NewCompilationDto;
import ru.practicum.main.dto.request.UpdateCompilationRequest;
import ru.practicum.main.dto.response.CompilationDto;
import ru.practicum.main.dto.response.EventShortDto;
import ru.practicum.main.service.CompilationService;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminCompilationController.class)
class AdminCompilationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CompilationService compilationService;

    private NewCompilationDto newCompilationDto;
    private CompilationDto compilationDto;
    private UpdateCompilationRequest updateRequest;

    @BeforeEach
    void setUp() {

        EventShortDto eventShortDto = EventShortDto.builder()
                .id(1L)
                .title("Test Event")
                .annotation("Test Annotation")
                .paid(false)
                .build();

        newCompilationDto = NewCompilationDto.builder()
                .title("Test Compilation")
                .pinned(true)
                .events(List.of(1L, 2L))
                .build();

        compilationDto = CompilationDto.builder()
                .id(1L)
                .title("Test Compilation")
                .pinned(true)
                .events(List.of(eventShortDto))
                .build();

        updateRequest = UpdateCompilationRequest.builder()
                .title("Updated Compilation")
                .pinned(false)
                .events(List.of(1L))
                .build();
    }

    @Test
    void createCompilation_ShouldReturnCreated() throws Exception {
        when(compilationService.createCompilation(any(NewCompilationDto.class)))
                .thenReturn(compilationDto);

        mockMvc.perform(post("/admin/compilations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newCompilationDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.title").value("Test Compilation"))
                .andExpect(jsonPath("$.pinned").value(true));
    }

    @Test
    void createCompilation_WithEmptyTitle_ShouldReturnBadRequest() throws Exception {
        NewCompilationDto invalidDto = NewCompilationDto.builder()
                .title("")
                .pinned(true)
                .build();

        mockMvc.perform(post("/admin/compilations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createCompilation_WithTitleTooLong_ShouldReturnBadRequest() throws Exception {
        String longTitle = "a".repeat(51);
        NewCompilationDto invalidDto = NewCompilationDto.builder()
                .title(longTitle)
                .pinned(true)
                .build();

        mockMvc.perform(post("/admin/compilations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateCompilation_ShouldReturnOk() throws Exception {
        Long compId = 1L;
        when(compilationService.updateCompilation(eq(compId), any(UpdateCompilationRequest.class)))
                .thenReturn(compilationDto);

        mockMvc.perform(patch("/admin/compilations/{compId}", compId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.title").value("Test Compilation"));
    }

    @Test
    void updateCompilation_WithInvalidTitle_ShouldReturnBadRequest() throws Exception {
        Long compId = 1L;
        UpdateCompilationRequest invalidRequest = UpdateCompilationRequest.builder()
                .title("") // Пустой title
                .build();

        mockMvc.perform(patch("/admin/compilations/{compId}", compId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteCompilation_ShouldReturnNoContent() throws Exception {
        Long compId = 1L;
        doNothing().when(compilationService).deleteCompilation(compId);

        mockMvc.perform(delete("/admin/compilations/{compId}", compId))
                .andExpect(status().isNoContent());
    }

    @Test
    void updateCompilation_WithTitleTooLong_ShouldReturnBadRequest() throws Exception {
        Long compId = 1L;
        String longTitle = "a".repeat(51);
        UpdateCompilationRequest invalidRequest = UpdateCompilationRequest.builder()
                .title(longTitle)
                .build();

        mockMvc.perform(patch("/admin/compilations/{compId}", compId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }
}