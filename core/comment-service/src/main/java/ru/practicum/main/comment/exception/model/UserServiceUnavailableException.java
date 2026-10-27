package ru.practicum.main.comment.exception.model;

public class UserServiceUnavailableException extends RuntimeException {
    public UserServiceUnavailableException(Long userId, Throwable cause) {
        super(
                "Сервис пользователей недоступен! Определено в рамках запроса по пользователю id=%d".formatted(userId),
                cause
        );
    }
}
