package ru.practicum.main.dto.client;

import lombok.Builder;

@Builder
public record UserShortDto(
        Long id,
        String name
) {
}