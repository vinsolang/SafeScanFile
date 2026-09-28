package com.backend.backend.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * Maps domain exceptions to HTTP responses for the REST API (health now,
 * admin dashboard and payment webhook later). The Telegram bot handles
 * the same exceptions itself and replies in chat.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InsufficientCreditsException.class)
    public ResponseEntity<Map<String, String>> credits(InsufficientCreditsException e) {
        return body(HttpStatus.PAYMENT_REQUIRED, e.getMessage());
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<Map<String, String>> rateLimit(RateLimitExceededException e) {
        return body(HttpStatus.TOO_MANY_REQUESTS, e.getMessage());
    }

    @ExceptionHandler(UserBlockedException.class)
    public ResponseEntity<Map<String, String>> blocked(UserBlockedException e) {
        return body(HttpStatus.FORBIDDEN, e.getMessage());
    }

    @ExceptionHandler({FileTooLargeException.class, InvalidFileException.class})
    public ResponseEntity<Map<String, String>> badFile(SafeScanException e) {
        return body(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(ScannerUnavailableException.class)
    public ResponseEntity<Map<String, String>> scanner(ScannerUnavailableException e) {
        log.warn("Scanner unavailable: {}", e.getMessage());
        return body(HttpStatus.SERVICE_UNAVAILABLE, "Scanner unavailable");
    }


    private static ResponseEntity<Map<String, String>> body(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of("error", message));
    }
}

