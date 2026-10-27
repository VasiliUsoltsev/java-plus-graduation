package ru.practicum.main.exception.model;

public class RequestServiceUnavailableException extends RuntimeException {
    public RequestServiceUnavailableException(Long eventId, Throwable cause) {
        super(
                ("Сервис запросов на добавление пользователей недоступен!" +
                        " Определено в рамках запроса по событию id=%d").formatted(eventId),
                cause
        );
    }

    public RequestServiceUnavailableException(Throwable cause) {
        super(
                "Сервис запросов на добавление пользователей недоступен! ", cause);
    }
}
