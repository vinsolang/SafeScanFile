package com.backend.backend.config;

import com.backend.backend.models.Plan;
import com.backend.backend.repository.PlanRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

@Slf4j
@Configuration
public class PlanSeeder {

    @Bean
    ApplicationRunner seedPlans(PlanRepository plans) {
        return args -> {
            seed(plans, "Free",    "0.00", 3,   36500);
            seed(plans, "Starter", "1.00", 20,  30);
            seed(plans, "Basic",   "3.00", 100, 30);
            seed(plans, "Pro",     "5.00", 300, 30);
        };
    }

    private void seed(PlanRepository plans, String name, String price, int scans, int days) {
        if (plans.findByNameIgnoreCase(name).isPresent()) {
            return;
        }
        Plan p = new Plan();
        p.setName(name);
        p.setPrice(new BigDecimal(price));
        p.setScanLimit(scans);
        p.setDurationDays(days);
        p.setActive(true);
        plans.save(p);
        log.info("Seeded plan {}", name);
    }
}
