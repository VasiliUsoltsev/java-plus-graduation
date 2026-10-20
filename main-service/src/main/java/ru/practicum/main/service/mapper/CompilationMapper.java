package ru.practicum.main.service.mapper;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import ru.practicum.main.dto.response.CompilationDto;
import ru.practicum.main.dto.response.EventShortDto;
import ru.practicum.main.model.Compilation;

import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CompilationMapper {
    public static CompilationDto toDto(Compilation compilation) {
        if (compilation == null) {
            return null;
        }

        List<EventShortDto> eventShortDtos = compilation.getEvents().stream()
                .map(event -> EventMapper.toShortDto(event, 0L, 0L, 0L))
                .toList();

        return CompilationDto.builder()
                .id(compilation.getId())
                .title(compilation.getTitle())
                .pinned(compilation.getPinned())
                .events(eventShortDtos)
                .build();
    }
}
