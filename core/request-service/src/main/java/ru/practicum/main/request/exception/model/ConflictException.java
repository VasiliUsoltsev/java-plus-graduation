package ru.practicum.main.request.exception.model;

public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
