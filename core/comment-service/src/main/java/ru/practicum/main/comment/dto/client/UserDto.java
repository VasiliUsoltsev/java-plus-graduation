package ru.practicum.main.comment.dto.client;

import lombok.Builder;

@Builder
public record UserDto(
        Long id,
        String name
) {
}
