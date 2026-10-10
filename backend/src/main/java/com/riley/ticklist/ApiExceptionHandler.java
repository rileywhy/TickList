package com.riley.ticklist;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.unit.DataSize;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.server.ResponseStatusException;

/**
 * Translates exceptions escaping any controller into client-shaped responses,
 * so internals (stack traces, Hibernate messages) never cross the API boundary.
 *
 * <p>Error bodies share one shape, {@code {"message": "..."}}, so the frontend can
 * display any of them with the same code. Bean-validation errors are the one
 * exception: they return a message per field.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    private final DataSize maxUploadSize;

    /**
     * @param maxUploadSize the per-file multipart limit, read from
     *     {@code spring.servlet.multipart.max-file-size} so the message shown to the
     *     user and the limit Spring enforces cannot drift apart
     */
    public ApiExceptionHandler(@Value("${spring.servlet.multipart.max-file-size}") DataSize maxUploadSize) {
        this.maxUploadSize = maxUploadSize;
    }

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

    /**
     * Handles an upload that exceeds the multipart file-size limit.
     *
     * <p>Spring rejects an oversized upload while parsing the request, before any
     * controller runs, so {@code ImportController} never sees it. Without this
     * handler the client receives a 413 with an empty body and can only report
     * the status code.
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.PAYLOAD_TOO_LARGE)
    public Map<String, String> onUploadTooLarge(MaxUploadSizeExceededException ex) {
        return Map.of("message",
            "That file is larger than the " + maxUploadSize.toMegabytes() + " MB upload limit.");
    }

    /**
     * Returns a controller's {@link ResponseStatusException} reason as the message.
     *
     * <p>Boot's default error page omits the reason
     * ({@code server.error.include-message=never}), so messages written for the
     * user, such as "CSV file does not match Mountain Project or Kaya format",
     * never reached the screen. Reasons in this codebase are written for clients
     * and contain no internals, so returning them is safe.
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> onResponseStatus(ResponseStatusException ex) {
        String message = ex.getReason() != null ? ex.getReason() : ex.getStatusCode().toString();
        return ResponseEntity.status(ex.getStatusCode()).body(Map.of("message", message));
    }
}
