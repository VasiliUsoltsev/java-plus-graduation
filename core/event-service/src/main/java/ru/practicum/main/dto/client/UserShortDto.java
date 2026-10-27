package ru.practicum.main.dto.client;

import lombok.*;

@Builder
public record UserShortDto (
    Long id,
    String name
) {}