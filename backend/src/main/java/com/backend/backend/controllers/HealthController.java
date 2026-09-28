package com.backend.backend.controllers;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.backend.backend.scanner.FileScannerService;

@RestController
@RequestMapping("/api")
public class HealthController {
    
    private final FileScannerService scanner;

    public HealthController(FileScannerService scanner) {
        this.scanner = scanner;
    }

    /** Liveness plus whether the antivirus engine is reachable. */
    @GetMapping("/health")
    public Map<String, String> health() {
        boolean scannerUp = scanner.isAvailable();
        return Map.of(
                "status", "UP",
                "scanner", scannerUp ? "UP" : "DOWN"
        );
    }
}