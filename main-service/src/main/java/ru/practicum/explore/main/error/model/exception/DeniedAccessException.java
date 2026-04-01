package ru.practicum.explore.main.error.model.exception;

public class DeniedAccessException extends RuntimeException {
    public DeniedAccessException(String message) {
        super(message);
    }
}
