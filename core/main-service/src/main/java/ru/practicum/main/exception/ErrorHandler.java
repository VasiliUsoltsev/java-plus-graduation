package ru.practicum.main.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.validation.BindException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import ru.practicum.main.exception.model.AccessDeniedException;
import ru.practicum.main.exception.model.ConflictException;
import ru.practicum.main.exception.model.NotFoundException;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class ErrorHandler {
    private static final String VALIDATION_ERROR = "Ошибка валидации";

    private ApiError buildApiError(String error, String message, HttpStatus status, HttpServletRequest request) {
        return new ApiError(
                error,
                message,
                status.name(),
                LocalDateTime.now(ZoneId.systemDefault()),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleMethodArgumentNotValid(final MethodArgumentNotValidException e, HttpServletRequest request) {
        String errorMessage = e.getBindingResult().getFieldErrors().stream()
                .map(error -> String.format("%s: %s", error.getField(), error.getDefaultMessage()))
                .collect(Collectors.joining(", "));

        log.error(VALIDATION_ERROR + ": {}", errorMessage);
        return buildApiError(VALIDATION_ERROR, errorMessage, HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleIllegalArgument(final IllegalArgumentException e, HttpServletRequest request) {
        log.error(VALIDATION_ERROR + ": {}", e.getMessage());
        return buildApiError(VALIDATION_ERROR, e.getMessage(), HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleMissingParams(final MissingServletRequestParameterException e, HttpServletRequest request) {
        log.error("Пропущен обязательный параметр: {}", e.getMessage());
        return buildApiError("Пропущен обязательный параметр", e.getMessage(), HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleMismatch(final MethodArgumentTypeMismatchException e, HttpServletRequest request) {
        log.error("Несовпадение типов: {}", e.getMessage());
        return buildApiError("Неверный формат параметра", e.getMessage(), HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleBind(final BindException e, HttpServletRequest request) {
        String errorMessage = e.getBindingResult().getFieldErrors().stream()
                .map(error -> String.format("%s: %s", error.getField(), error.getDefaultMessage()))
                .collect(Collectors.joining(", "));
        log.error(VALIDATION_ERROR + ": {}", errorMessage);
        return buildApiError(VALIDATION_ERROR, errorMessage, HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleConstraintViolation(final ConstraintViolationException e, HttpServletRequest request) {
        String errorMessage = e.getConstraintViolations().stream()
                .map(violation -> String.format("%s: %s",
                        violation.getPropertyPath().toString(),
                        violation.getMessage()))
                .collect(Collectors.joining(", "));

        log.error(VALIDATION_ERROR + ": {}", errorMessage);
        return buildApiError(VALIDATION_ERROR, errorMessage, HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiError handleAllExceptions(final Exception e, HttpServletRequest request) {
        log.error("Непредвиденная ошибка: {}", e.getMessage(), e);
        return buildApiError("Внутренняя ошибка сервера", "Произошла непредвиденная ошибка",
                HttpStatus.INTERNAL_SERVER_ERROR, request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleHttpMessageNotReadable(HttpMessageNotReadableException e, HttpServletRequest r) {
        log.error("Неверный формат JSON: {}", e.getMessage());
        return buildApiError("Неверный формат запроса", e.getMessage(), HttpStatus.BAD_REQUEST, r);
    }

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiError handleNotFound(final NotFoundException e, HttpServletRequest request) {
        log.error("Объект не найден: {}", e.getMessage());
        return buildApiError("The required object was not found.",
                e.getMessage(), HttpStatus.NOT_FOUND, request);
    }

    @ExceptionHandler(ConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError handleConflict(final ConflictException e, HttpServletRequest request) {
        log.error("Конфликт данных: {}", e.getMessage());
        return buildApiError("For the requested operation the conditions are not met.",
                e.getMessage(), HttpStatus.CONFLICT, request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError handleDataIntegrityViolation(final DataIntegrityViolationException e, HttpServletRequest request) {
        log.error("Нарушение целостности данных: {}", e.getMessage());

        String message = "For the requested operation the conditions are not met.";


        if (e.getMessage() != null &&
                (e.getMessage().contains("UK_compilation_title") ||
                        e.getMessage().contains("duplicate key"))) {
            message = "Compilation with this title already exists";
        }

        return buildApiError(
                "For the requested operation the conditions are not met.",
                message,
                HttpStatus.CONFLICT,
                request
        );
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiError handleAccess(final AccessDeniedException e, HttpServletRequest request) {
        log.error("Отсутствует доступ: {}", e.getMessage());
        return buildApiError("Access is unavailable.",
                e.getMessage(), HttpStatus.FORBIDDEN, request);
    }
}