package com.backend.backend.exception;

public class UserBlockedException extends SafeScanException {

    public UserBlockedException() {
        super("This account has been blocked");
    }
}

