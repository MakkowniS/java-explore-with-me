package ru.practicum.explore.main.error.model.exception;

// Status 404
public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
}
