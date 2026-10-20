package ru.practicum.main.compilations;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.main.controller.PublicCompilationController;
import ru.practicum.main.dto.response.CompilationDto;
import ru.practicum.main.dto.response.EventShortDto;
import ru.practicum.main.service.CompilationService;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PublicCompilationController.class)
class PublicCompilationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CompilationService compilationService;

    private CompilationDto compilationDto;
    private List<CompilationDto> compilations;

    @BeforeEach
    void setUp() {
        EventShortDto eventShortDto = EventShortDto.builder()
                .id(1L)
                .title("Test Event")
                .annotation("Test Annotation")
                .paid(false)
                .build();

        compilationDto = CompilationDto.builder()
                .id(1L)
                .title("Test Compilation")
                .pinned(true)
                .events(List.of(eventShortDto))
                .build();

        compilations = List.of(compilationDto);
    }

    @Test
    void getCompilations_WithoutFilters_ShouldReturnList() throws Exception {
        when(compilationService.getCompilations(eq(null), any(Pageable.class)))
                .thenReturn(compilations);

        mockMvc.perform(get("/compilations")
                        .param("from", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].title").value("Test Compilation"))
                .andExpect(jsonPath("$[0].pinned").value(true));
    }

    @Test
    void getCompilations_WithPinnedFilter_ShouldReturnFilteredList() throws Exception {
        when(compilationService.getCompilations(eq(true), any(Pageable.class)))
                .thenReturn(compilations);

        mockMvc.perform(get("/compilations")
                        .param("pinned", "true")
                        .param("from", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].pinned").value(true));
    }

    @Test
    void getCompilations_WithCustomPagination_ShouldReturnCorrectPage() throws Exception {
        Pageable pageable = PageRequest.of(1, 5);
        when(compilationService.getCompilations(eq(null), any(Pageable.class)))
                .thenReturn(compilations);

        mockMvc.perform(get("/compilations")
                        .param("from", "5")
                        .param("size", "5"))
                .andExpect(status().isOk());
    }

    @Test
    void getCompilationById_ShouldReturnCompilation() throws Exception {
        Long compId = 1L;
        when(compilationService.getCompilationById(compId))
                .thenReturn(compilationDto);

        mockMvc.perform(get("/compilations/{compId}", compId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.title").value("Test Compilation"));
    }

    @Test
    void getCompilations_WithDefaultPagination_ShouldWork() throws Exception {
        when(compilationService.getCompilations(eq(null), any(Pageable.class)))
                .thenReturn(compilations);

        mockMvc.perform(get("/compilations"))
                .andExpect(status().isOk());
    }
}