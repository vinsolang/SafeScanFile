package com.backend.backend.dto;

import java.io.InputStream;

/**
 * A file to scan, independent of where it came from (Telegram today,
 * possibly a web upload later). {@code content} is only invoked once
 * validation and credit checks have passed, so nothing is downloaded
 * for requests that will be rejected.
 *
 * @param size declared size in bytes, or null if the source didn't say
 *             (the real limit is enforced while copying anyway)
 */
public record IncomingFile(
        String fileName,
        Long size,
        String mimeType,
        IoSupplier<InputStream> content) {
}
