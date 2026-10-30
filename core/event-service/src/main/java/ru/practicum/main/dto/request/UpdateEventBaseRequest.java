package ru.practicum.main.dto.request;

import ru.practicum.main.model.Location;

import java.time.LocalDateTime;

public interface UpdateEventBaseRequest {
    String getAnnotation();

    Long getCategory();

    String getDescription();

    LocalDateTime getEventDate();

    Location getLocation();

    Boolean getPaid();

    Integer getParticipantLimit();

    Boolean getRequestModeration();

    String getTitle();
}