package ru.practicum.main.exception.model;

public class EventCommentServiceUnavailableException extends RuntimeException {
    public EventCommentServiceUnavailableException(Long eventId, Throwable cause) {
        super(
                "Сервис комментариев недоступен! Определено в рамках запроса по пользователю id=%d".formatted(eventId),
                cause
        );
    }

    public EventCommentServiceUnavailableException(Throwable cause) {
        super(
                "Сервис комментариев недоступен! ", cause);
    }
}
