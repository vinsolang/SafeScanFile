package com.backend.backend.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.backend.backend.config.SafeScanProperties;
import com.backend.backend.exception.FileTooLargeException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * Owns the temporary directory. Uploaded files only ever exist here, under
 * random names, only for the duration of a scan. They are never executed
 * or opened, and a scheduled sweep removes anything left by a crash.
 */
@Service
public class TempFileService {

    private static final Duration STALE_AFTER = Duration.ofHours(1);

    private final Path dir;
    private final long maxBytes;

    @Autowired
    public TempFileService(SafeScanProperties props) {
        this(Path.of(props.scanner().tmpDir()), props.scanner().maxFileSizeBytes());
    }

    public TempFileService(Path dir, long maxBytes) {
        this.dir = dir;
        this.maxBytes = maxBytes;
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot create temp dir " + dir, e);
        }
    }

    /** New empty file with a random name (owner-only permissions on POSIX). */
    public Path createTempFile() throws IOException {
        return Files.createTempFile(dir, "scan-", ".tmp");
    }

    /**
     * Copies the stream into {@code target}, aborting if it exceeds the
     * size limit. The declared size from Telegram is not trusted.
     *
     * @return number of bytes written
     */
    public long writeBounded(InputStream in, Path target) throws IOException {
        long total = 0;
        byte[] buf = new byte[8192];
        try (InputStream source = in; OutputStream out = Files.newOutputStream(target)) {
            int n;
            while ((n = source.read(buf)) != -1) {
                total += n;
                if (total > maxBytes) {
                    throw new FileTooLargeException(maxBytes);
                }
                out.write(buf, 0, n);
            }
        }
        return total;
    }

    public void deleteQuietly(Path file) {
        if (file == null) {
            return;
        }
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            // A leftover file is picked up by the scheduled sweep.
        }
    }

    /** Removes files older than {@code age}; returns how many were deleted. */
    public int purgeOlderThan(Duration age) {
        Instant cutoff = Instant.now().minus(age);
        int deleted = 0;
        try (Stream<Path> files = Files.list(dir)) {
            for (Path p : (Iterable<Path>) files::iterator) {
                try {
                    if (Files.isRegularFile(p) && Files.getLastModifiedTime(p).toInstant().isBefore(cutoff)
                            && Files.deleteIfExists(p)) {
                        deleted++;
                    }
                } catch (IOException e) {
                    // skip, try again next sweep
                }
            }
        } catch (IOException e) {
            // directory unreadable this round
        }
        return deleted;
    }

    @Scheduled(fixedDelay = 15, timeUnit = TimeUnit.MINUTES)
    public void purgeStaleFiles() {
        purgeOlderThan(STALE_AFTER);
    }
}

