package ru.practicum.eventservice.exception;

public class CategoryNotFoundException extends RuntimeException {
    public CategoryNotFoundException(Long catId) {
        super("Category with id=" + catId + " was not found");
    }
}
