package com.backend.backend.dto;

import com.backend.backend.models.ScanStatus;

/**
 * What the user gets back. When {@code status} is ERROR the credit has
 * already been refunded.
 */
public record ScanReport(
        String fileName,
        long fileSize,
        String sha256,
        ScanStatus status,
        String threatName,
        long remainingCredits) {
}
