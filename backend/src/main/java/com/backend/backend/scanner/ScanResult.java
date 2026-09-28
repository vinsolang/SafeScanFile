package com.backend.backend.scanner;

import com.backend.backend.models.ScanStatus;

/**
 * Outcome of scanning one file. Only CLEAN, THREAT_FOUND and ERROR are
 * ever produced by a scanner; PENDING/SCANNING are lifecycle states.
 */
public record ScanResult(ScanStatus status, String threatName, String detail) {

    public static ScanResult clean() {
        return new ScanResult(ScanStatus.CLEAN, null, null);
    }

    public static ScanResult threat(String threatName) {
        return new ScanResult(ScanStatus.THREAT_FOUND, threatName, null);
    }

    public static ScanResult error(String detail) {
        return new ScanResult(ScanStatus.ERROR, null, detail);
    }
} 