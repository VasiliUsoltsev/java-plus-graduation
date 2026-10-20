package ru.practicum.main.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum EventState {
    PENDING,
    PUBLISHED,
    CANCELED;

    @JsonCreator
    public static EventState from(String value) {
        for (EventState state : values()) {
            if (state.name().equalsIgnoreCase(value)) {
                return state;
            }
        }
        throw new IllegalArgumentException("Unknown event state: " + value);
    }
}