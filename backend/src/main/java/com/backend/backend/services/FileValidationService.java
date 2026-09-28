package com.backend.backend.services;

import org.springframework.stereotype.Service;

import com.backend.backend.config.SafeScanProperties;
import com.backend.backend.exception.FileTooLargeException;
import com.backend.backend.exception.InvalidFileException;

/**
 * Cheap pre-checks that run before any credit is spent or byte downloaded.
 *
 * Deliberately no extension/MIME allow- or deny-list: a malware scanner
 * exists to inspect arbitrary files (.exe, .zip, .docm ...), and both the
 * extension and the client-declared MIME type are attacker-controlled.
 * They are recorded for display/history only, never used for decisions.
 */
@Service
public class FileValidationService {

    private static final int MAX_NAME_LENGTH = 255;

    private final long maxBytes;

    public FileValidationService(SafeScanProperties props) {
        this.maxBytes = props.scanner().maxFileSizeBytes();
    }

    public void validateDeclaredSize(Long size) {
        if (size == null) {
            return; // enforced again while copying
        }
        if (size <= 0) {
            throw new InvalidFileException("The file is empty");
        }
        if (size > maxBytes) {
            throw new FileTooLargeException(maxBytes);
        }
    }

    /**
     * Makes a user-supplied name safe to store and echo back: no path parts,
     * no control characters, and no invisible "format" characters such as the
     * right-to-left override (U+202E) used to disguise "invoice&lt;RLO&gt;fdp.exe".
     */
    public String sanitizeFileName(String name) {
        if (name == null) {
            return "unnamed";
        }
        String n = name.replaceAll("[\\p{Cntrl}\\p{Cf}]", "");
        int slash = Math.max(n.lastIndexOf('/'), n.lastIndexOf('\\'));
        if (slash >= 0) {
            n = n.substring(slash + 1);
        }
        n = n.strip();
        if (n.length() > MAX_NAME_LENGTH) {
            n = n.substring(0, MAX_NAME_LENGTH);
        }
        return n.isEmpty() ? "unnamed" : n;
    }
}
