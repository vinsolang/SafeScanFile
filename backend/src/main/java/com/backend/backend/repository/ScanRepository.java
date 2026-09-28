package com.backend.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.backend.backend.models.Scan;

import java.util.List;
import java.util.Optional;

public interface ScanRepository extends JpaRepository<Scan, Long> {

    List<Scan> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<Scan> findFirstBySha256OrderByCreatedAtDesc(String sha256);
}
