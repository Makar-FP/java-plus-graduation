package ru.practicum.eventservice.exception;

public class EventsGetPublicBadRequestException extends RuntimeException {
    public EventsGetPublicBadRequestException() {
        super("Events not found.");
    }
}
