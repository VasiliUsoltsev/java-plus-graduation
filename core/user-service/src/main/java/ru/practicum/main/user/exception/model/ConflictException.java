package ru.practicum.main.user.exception.model;

public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
