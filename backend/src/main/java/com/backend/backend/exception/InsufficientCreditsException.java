package com.backend.backend.exception;

public class InsufficientCreditsException extends SafeScanException {

    public InsufficientCreditsException() {
        super("No scan credits remaining");
    }
}
