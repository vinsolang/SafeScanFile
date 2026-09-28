package com.backend.backend.scanner;

import java.nio.file.Path;

public interface FileScannerService {

    /**
     * Scans the file without ever executing or opening it.
     * Never throws for scanner problems: returns {@link ScanResult#error}
     * so the caller can refund the credit and tell the user.
     */
    ScanResult scan(Path file);
    boolean isAvailable();
    /** True if the scanning engine is reachable and ready. */
}
