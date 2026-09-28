package com.backend.backend.controllers;

import com.backend.backend.dto.Request.AddCreditsRequest;
import com.backend.backend.dto.Request.PlanRequest;
import com.backend.backend.dto.Request.StatusUpdateRequest;
import com.backend.backend.models.Payment;
import com.backend.backend.models.Plan;
import com.backend.backend.models.Scan;
import com.backend.backend.models.ScanStatus;
import com.backend.backend.models.Subscription;
import com.backend.backend.models.SubscriptionStatus;
import com.backend.backend.models.User;
import com.backend.backend.repository.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "http://localhost:5173") // Enables React Vite CORS
public class AdminController {

    private final UserRepository userRepository;
    private final PlanRepository planRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final PaymentRepository paymentRepository;
    private final ScanRepository scanRepository;

    public AdminController(
            UserRepository userRepository,
            PlanRepository planRepository,
            SubscriptionRepository subscriptionRepository,
            PaymentRepository paymentRepository,
            ScanRepository scanRepository) {
        this.userRepository = userRepository;
        this.planRepository = planRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.paymentRepository = paymentRepository;
        this.scanRepository = scanRepository;
    }

    // ==================== STATS ====================
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", userRepository.count());
        stats.put("totalScans", scanRepository.count());
        stats.put("totalThreats", scanRepository.countByScanStatus(ScanStatus.THREAT_FOUND));
        stats.put("activeSubscriptions", subscriptionRepository.countByStatus(SubscriptionStatus.ACTIVE));
        return ResponseEntity.ok(stats);
    }

    // ==================== USERS ====================
    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userRepository.findAll());
    }

    @PatchMapping("/users/{id}/status")
    public ResponseEntity<User> updateUserStatus(
            @PathVariable Long id,
            @RequestBody StatusUpdateRequest request) {
        return userRepository.findById(id).map(user -> {
            user.setStatus(request.status());
            return ResponseEntity.ok(userRepository.save(user));
        }).orElse(ResponseEntity.notFound().build());
    }

    // ==================== PLANS ====================
    @GetMapping("/plans")
    public ResponseEntity<List<Plan>> getAllPlans() {
        return ResponseEntity.ok(planRepository.findAll());
    }

    @PostMapping("/plans")
    public ResponseEntity<Plan> createPlan(@RequestBody PlanRequest request) {
        Plan plan = new Plan();
        plan.setName(request.name());
        plan.setPrice(request.price());
        plan.setScanLimit(request.scanLimit());
        plan.setDurationDays(request.durationDays());
        plan.setActive(request.active() != null ? request.active() : true);

        return ResponseEntity.ok(planRepository.save(plan));
    }

    @PutMapping("/plans/{id}")
    public ResponseEntity<Plan> updatePlan(
            @PathVariable Long id,
            @RequestBody PlanRequest request) {
        return planRepository.findById(id).map(plan -> {
            plan.setName(request.name());
            plan.setPrice(request.price());
            plan.setScanLimit(request.scanLimit());
            plan.setDurationDays(request.durationDays());
            if (request.active() != null) {
                plan.setActive(request.active());
            }
            return ResponseEntity.ok(planRepository.save(plan));
        }).orElse(ResponseEntity.notFound().build());
    }

    // ==================== SUBSCRIPTIONS ====================
    @GetMapping("/subscriptions")
    public ResponseEntity<List<Subscription>> getAllSubscriptions() {
        return ResponseEntity.ok(subscriptionRepository.findAll());
    }

    @PostMapping("/subscriptions/{id}/add-credits")
    public ResponseEntity<Subscription> addCredits(
            @PathVariable Long id,
            @RequestBody AddCreditsRequest request) {
        return subscriptionRepository.findById(id).map(sub -> {
            sub.setCredits(sub.getCredits() + request.credits());
            return ResponseEntity.ok(subscriptionRepository.save(sub));
        }).orElse(ResponseEntity.notFound().build());
    }

    // ==================== PAYMENTS ====================
    @GetMapping("/payments")
    public ResponseEntity<List<Payment>> getAllPayments() {
        return ResponseEntity.ok(paymentRepository.findAll());
    }

    // ==================== SCANS ====================
    @GetMapping("/scans")
    public ResponseEntity<List<Scan>> getRecentScans() {
        return ResponseEntity.ok(scanRepository.findTop100ByOrderByCreatedAtDesc());
    }
}
