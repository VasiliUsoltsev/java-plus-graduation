package ru.practicum.main.service;

import ru.practicum.main.dto.request.NewCompilationDto;
import ru.practicum.main.dto.request.UpdateCompilationRequest;
import ru.practicum.main.dto.response.CompilationDto;

import org.springframework.data.domain.Pageable;
import java.util.List;

public interface CompilationService {
    CompilationDto createCompilation(NewCompilationDto newCompilationDto);

    CompilationDto updateCompilation(Long compId, UpdateCompilationRequest request);

    void deleteCompilation(Long compId);

    List<CompilationDto> getCompilations(Boolean pinned, Pageable pageable);

    CompilationDto getCompilationById(Long compId);
}
