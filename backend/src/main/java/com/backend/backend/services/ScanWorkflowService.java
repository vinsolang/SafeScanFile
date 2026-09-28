package com.backend.backend.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import com.backend.backend.dto.IncomingFile;
import com.backend.backend.dto.ScanReport;
import com.backend.backend.exception.FileDownloadException;
import com.backend.backend.exception.UserBlockedException;
import com.backend.backend.models.Scan;
import com.backend.backend.models.ScanStatus;
import com.backend.backend.models.User;
import com.backend.backend.models.UserStatus;
import com.backend.backend.repository.ScanRepository;
import com.backend.backend.scanner.FileScannerService;
import com.backend.backend.scanner.ScanResult;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;

/**
 * The scan pipeline (Step 12):
 *
 *   check user -> rate limit -> validate size -> spend credit
 *   -> download to temp file -> SHA-256 -> ClamAV -> save result
 *   -> always delete temp file -> refund credit if no verdict
 *
 * Deliberately not @Transactional as a whole: it does network I/O, and a
 * database transaction must not stay open across a download and a scan.
 * The credit spend/refund and the result insert are each their own
 * short transaction.
 */
@Slf4j
@Service
public class ScanWorkflowService {

    private final RateLimitService rateLimit;
    private final FileValidationService validation;
    private final CreditService credits;
    private final TempFileService tempFiles;
    private final HashService hashes;
    private final FileScannerService scanner;
    private final ScanRepository scans;

    public ScanWorkflowService(RateLimitService rateLimit, FileValidationService validation,
                               CreditService credits, TempFileService tempFiles,
                               HashService hashes, FileScannerService scanner,
                               ScanRepository scans) {
        this.rateLimit = rateLimit;
        this.validation = validation;
        this.credits = credits;
        this.tempFiles = tempFiles;
        this.hashes = hashes;
        this.scanner = scanner;
        this.scans = scans;
    }

    public ScanReport scanFile(User user, IncomingFile file) {
        if (user.getStatus() == UserStatus.BLOCKED) {
            throw new UserBlockedException();
        }
        rateLimit.checkScanAllowed(user.getTelegramUserId());
        validation.validateDeclaredSize(file.size());
        String fileName = validation.sanitizeFileName(file.fileName());

        Long chargedSubscription = credits.consume(user.getId());
        boolean refund = true; // stays true until a real verdict is stored
        Path tmp = null;
        long size;
        String sha256;
        ScanResult result;
        try {
            tmp = tempFiles.createTempFile();
            size = download(file, tmp);
            sha256 = hashes.sha256(tmp);
            result = scanner.scan(tmp);

            Scan scan = new Scan();
            scan.setUser(user);
            scan.setFileName(fileName);
            scan.setFileSize(size);
            scan.setMimeType(file.mimeType());
            scan.setSha256(sha256);
            scan.setScanStatus(result.status());
            scan.setThreatName(result.threatName());
            scans.save(scan);

            refund = result.status() == ScanStatus.ERROR;
        } catch (IOException e) {
            throw new FileDownloadException("Could not process the uploaded file", e);
        } finally {
            tempFiles.deleteQuietly(tmp);
            if (refund) {
                refundQuietly(chargedSubscription, user);
            }
        }

        long remaining = credits.balance(user.getId());
        return new ScanReport(fileName, size, sha256, result.status(), result.threatName(), remaining);
    }

    public List<Scan> recentScans(Long userId, int limit) {
        return scans.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public long balance(Long userId) {
        return credits.balance(userId);
    }

    private long download(IncomingFile file, Path target) throws IOException {
        try (InputStream in = file.content().get()) {
            return tempFiles.writeBounded(in, target);
        }
    }

    private void refundQuietly(Long subscriptionId, User user) {
        try {
            credits.refund(subscriptionId);
        } catch (RuntimeException e) {
            // Must not mask the original failure. Needs manual attention.
            log.error("CREDIT REFUND FAILED for user {} subscription {}", user.getId(), subscriptionId, e);
        }
    }
}
