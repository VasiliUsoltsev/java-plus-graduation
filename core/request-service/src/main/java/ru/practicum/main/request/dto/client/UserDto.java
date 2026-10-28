package ru.practicum.main.request.dto.client;

import lombok.Builder;

@Builder
public record UserDto(
        Long id,
        String name,
        String email
) {
}
