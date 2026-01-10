package ru.practicum.userservice.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(UserNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ExceptionResponse handleUserNotFound(UserNotFoundException ex) {
        return new ExceptionResponse(
                HttpStatus.NOT_FOUND.name(),
                "The required object was not found.",
                ex.getMessage()
        );
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ExceptionResponse handleException(final Exception e) {
        log.error("500 Internal Server Error: {}", e.getMessage(), e);
        return new ExceptionResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.name(),
                "Error occurred.",
                e.getMessage()
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ExceptionResponse handlerValidationException(MethodArgumentNotValidException ex) {
        return new ExceptionResponse(
                HttpStatus.BAD_REQUEST.name(),
                "Incorrectly made request.",
                ""
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ExceptionResponse handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return new ExceptionResponse(
                HttpStatus.BAD_REQUEST.name(),
                "Incorrectly made request.",
                ex.getMessage()
        );
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ExceptionResponse handleMissingRequestParam(MissingServletRequestParameterException ex) {
        return new ExceptionResponse(
                HttpStatus.BAD_REQUEST.name(),
                "Missing request parameter.",
                String.format("Required request parameter '%s' is missing.", ex.getParameterName())
        );
    }


    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ExceptionResponse handlerDataIntegrityViolation(DataIntegrityViolationException ex) {
        return new ExceptionResponse(
                HttpStatus.CONFLICT.name(),
                "Integrity constraint has been violated.",
                ex.getMessage()
        );
    }

}