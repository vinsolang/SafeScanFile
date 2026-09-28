package com.backend.backend.scanner;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import com.backend.backend.config.SafeScanProperties;
import com.backend.backend.exception.ScannerUnavailableException;

import java.nio.file.Path;

@Slf4j
@Service
public class ClamAvScannerService implements FileScannerService {

    private final ClamdClient client;

    private final SafeScanProperties properties;

    public ClamAvScannerService(SafeScanProperties props) {
        this.properties = props;
        var s = props.scanner();
        this.client = new ClamdClient(s.clamavHost(), s.clamavPort(), s.timeoutMs());
    }

    @Override
    public ScanResult scan(Path file) {
        try {
            return client.scan(file);
        } catch (ScannerUnavailableException e) {
            log.error("ClamAV scan failed: {}", e.getMessage());
            return ScanResult.error("Scanner unavailable");
        }
    }

    public boolean isAvailable() {
        return client.ping();
    }
}
