package com.backend.backend.exception;

/** Base type for all expected, user-facing failures in SafeScan. */
public class SafeScanException extends RuntimeException {

    public SafeScanException(String message) {
        super(message);
    }

    public SafeScanException(String message, Throwable cause) {
        super(message, cause);
    }
}