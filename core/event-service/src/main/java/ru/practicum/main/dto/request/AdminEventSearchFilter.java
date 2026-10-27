package ru.practicum.main.dto.request;

import ru.practicum.main.model.enums.EventState;

import java.time.LocalDateTime;
import java.util.List;

public record AdminEventSearchFilter(
        List<Long> users,
        List<EventState> states,
        List<Long> categories,
        LocalDateTime rangeStart,
        LocalDateTime rangeEnd
) {}
