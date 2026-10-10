package com.riley.ticklist;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.server.ResponseStatusException;

class ApiExceptionHandlerTest {

    private final ApiExceptionHandler handler = new ApiExceptionHandler(DataSize.ofMegabytes(25));

    @Test
    void uploadTooLargeMessageNamesTheConfiguredLimit() {
        Map<String, String> body = handler.onUploadTooLarge(
            new MaxUploadSizeExceededException(DataSize.ofMegabytes(25).toBytes()));

        assertThat(body).containsEntry("message", "That file is larger than the 25 MB upload limit.");
    }

    @Test
    void responseStatusReasonBecomesTheMessage() {
        ResponseEntity<Map<String, String>> response = handler.onResponseStatus(
            new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose a CSV file to import."));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("message", "Choose a CSV file to import.");
    }

    @Test
    void responseStatusWithoutReasonFallsBackToTheStatus() {
        ResponseEntity<Map<String, String>> response = handler.onResponseStatus(
            new ResponseStatusException(HttpStatus.NOT_FOUND));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).containsEntry("message", "404 NOT_FOUND");
    }
}
