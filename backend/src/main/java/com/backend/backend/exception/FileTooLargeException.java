package com.backend.backend.exception;

public class FileTooLargeException extends SafeScanException {

    private final long maxBytes;

    public FileTooLargeException(long maxBytes) {
        super("File exceeds the maximum allowed size of " + maxBytes + " bytes");
        this.maxBytes = maxBytes;
    }

    public long getMaxBytes() {
        return maxBytes;
    }
}
