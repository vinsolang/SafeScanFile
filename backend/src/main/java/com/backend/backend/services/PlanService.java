package com.backend.backend.services;

import org.springframework.stereotype.Service;

import com.backend.backend.models.Plan;
import com.backend.backend.repository.PlanRepository;

import java.util.Comparator;
import java.util.List;

@Service
public class PlanService {

    private final PlanRepository plans;

    public PlanService(PlanRepository plans) {
        this.plans = plans;
    }

    public List<Plan> activePlans() {
        return plans.findByActiveTrue().stream()
                .sorted(Comparator.comparing(Plan::getPrice))
                .toList();
    }
}

