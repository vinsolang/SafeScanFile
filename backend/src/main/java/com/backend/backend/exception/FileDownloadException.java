package com.backend.backend.exception;


public class FileDownloadException extends SafeScanException {

    public FileDownloadException(String message, Throwable cause) {
        super(message, cause);
    }
}