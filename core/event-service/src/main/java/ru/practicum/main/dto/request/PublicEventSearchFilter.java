package ru.practicum.main.dto.request;

import ru.practicum.main.model.enums.EventState;

import java.time.LocalDateTime;
import java.util.List;

public record PublicEventSearchFilter(
        String text,
        EventState state,
        List<Long> categories,
        Boolean paid,
        LocalDateTime rangeStart,
        LocalDateTime rangeEnd,
        Boolean onlyAvailable
) {}
