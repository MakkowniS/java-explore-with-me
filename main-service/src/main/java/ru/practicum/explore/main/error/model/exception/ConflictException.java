package ru.practicum.explore.main.error.model.exception;

// Status 409
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
