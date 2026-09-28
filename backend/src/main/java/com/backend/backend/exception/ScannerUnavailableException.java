package com.backend.backend.exception;

/** The antivirus engine could not be reached or returned an unusable reply. */
public class ScannerUnavailableException extends SafeScanException {

    public ScannerUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }

    public ScannerUnavailableException(String message) {
        super(message);
    }
}

