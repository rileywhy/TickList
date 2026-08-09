package com.riley.ticklist;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Translates exceptions escaping any controller into client-shaped responses,
 * so internals (stack traces, Hibernate messages) never cross the API boundary.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    /** Bean-validation failures from @Valid become a 400 with one message per field. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> onValidationError(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        // putIfAbsent keeps the first message when a field fails several rules
        // (e.g. a blank password is both @NotBlank and under @Size).
        ex.getBindingResult().getFieldErrors()
            .forEach(fieldError -> errors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage()));
        return errors;
    }
}
