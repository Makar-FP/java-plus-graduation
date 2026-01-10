package ru.practicum.requestservice.exception;

public class RequestNotFoundException extends RuntimeException {
    public RequestNotFoundException(Long requestId) {
        super("Compilation with id=" + requestId + " was not found");
    }
}
