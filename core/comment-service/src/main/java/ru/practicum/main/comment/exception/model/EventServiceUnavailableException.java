package ru.practicum.main.comment.exception.model;

public class EventServiceUnavailableException extends RuntimeException {
    public EventServiceUnavailableException(Long eventId, Throwable cause) {
        super(
                "Сервис событий недоступен! Определено в рамках запроса по событию id=%d".formatted(eventId),
                cause
        );
    }

    public EventServiceUnavailableException(Throwable cause) {
        super("Сервис событий недоступен!");
    }
}
