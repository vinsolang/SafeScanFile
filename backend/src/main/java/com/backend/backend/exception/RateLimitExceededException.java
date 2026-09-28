package com.backend.backend.exception;

public class RateLimitExceededException extends SafeScanException {

    public RateLimitExceededException() {
        super("Too many scan requests, please slow down");
    }
}

