package ru.practicum.explore.stat.server.error;

import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.practicum.explore.stat.server.error.validation.ValidationExceptionResponse;
import ru.practicum.explore.stat.server.error.validation.ValidationViolation;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class ErrorHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleIllegalArgumentException(IllegalArgumentException ex) {
        return Map.of("error", ex.getMessage());
    }

    // Обработка исключения валидации MethodArgumentNotValidException
    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ValidationExceptionResponse onMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        final List<ValidationViolation> violations = e.getBindingResult().getFieldErrors().stream()
                .map(error -> new ValidationViolation(error.getField(), error.getDefaultMessage()))
                .collect(Collectors.toList());
        return new ValidationExceptionResponse(violations);
    }

    // Ошибки параметров запроса (например, @Positive на @RequestParam)
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ValidationExceptionResponse onConstraintViolationException(ConstraintViolationException e) {
        final List<ValidationViolation> violations = e.getConstraintViolations().stream()
                .map(violation -> new ValidationViolation(violation.getPropertyPath().toString(), violation.getMessage()))
                .collect(Collectors.toList());
        return new ValidationExceptionResponse(violations);
    }

}
