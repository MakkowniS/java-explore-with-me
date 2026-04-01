package ru.practicum.explore.main.error;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import ru.practicum.explore.main.error.model.ApiError;
import ru.practicum.explore.main.error.model.exception.NotFoundException;
import ru.practicum.explore.main.error.model.exception.DeniedAccessException;

import java.time.LocalDateTime;

@RestControllerAdvice
@Slf4j
public class ErrorHandler {

    // 400 Ошибка конвертации типов
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        return ApiError.builder()
                .status(HttpStatus.BAD_REQUEST.name())
                .reason("Incorrectly made request.")
                .message("Failed to convert value of type " + e.getValue().getClass().getSimpleName() +
                        " to required type " + e.getRequiredType().getSimpleName() +
                        "; " + e.getMessage())
                .timestamp(LocalDateTime.now())
                .build();
    }

    // 400 Ошибка валидации
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleValidation(final MethodArgumentNotValidException e) {
        log.error("400: Validation error: {}", e.getMessage());
        FieldError fieldError = e.getBindingResult().getFieldError();
        // Формируем сообщение
        String message = String.format("Field: %s. Error: %s. Value: %s",
                fieldError.getField(), fieldError.getDefaultMessage(), fieldError.getRejectedValue());

        return ApiError.builder()
                .status(HttpStatus.BAD_REQUEST.name())
                .reason("Incorrectly made request.")
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }

    // 403 Ошибка доступа
    @ExceptionHandler(DeniedAccessException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiError handleTypeMismatch(DeniedAccessException e) {
        return ApiError.builder()
                .status(HttpStatus.FORBIDDEN.name())
                .reason("Access Denied.")
                .message(e.getMessage())
                .timestamp(LocalDateTime.now())
                .build();
    }

    // 404 NotFound
    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiError handleNotFound(final NotFoundException e) {
        log.error("404: {}", e.getMessage());
        return ApiError.builder()
                .status(HttpStatus.NOT_FOUND.name())
                .reason("The required object was not found.")
                .message(e.getMessage())
                .timestamp(LocalDateTime.now())
                .build();
    }

    // 409 Conflict (Нарушение уникальности БД)
    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError handleConflict(final DataIntegrityViolationException e) {
        log.error("409: Conflict: {}", e.getMessage());
        return ApiError.builder()
                .status(HttpStatus.CONFLICT.name())
                .reason("Integrity constraint has been violated.")
                .message(e.getMessage())
                .timestamp(LocalDateTime.now())
                .build();
    }

    // 500 Непредвиденная ошибка
    @ExceptionHandler(Throwable.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiError handleThrowable(final Throwable e) {
        log.error("500: Internal Server Error: {}", e.getMessage());
        return ApiError.builder()
                .status(HttpStatus.INTERNAL_SERVER_ERROR.name())
                .reason("An unexpected error occurred.")
                .message(e.getMessage())
                .timestamp(LocalDateTime.now())
                .build();
    }

}
