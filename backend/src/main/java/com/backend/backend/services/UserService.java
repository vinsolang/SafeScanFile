package com.backend.backend.services;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import com.backend.backend.models.Plan;
import com.backend.backend.models.User;
import com.backend.backend.models.UserStatus;
import com.backend.backend.repository.PlanRepository;
import com.backend.backend.repository.UserRepository;

import java.util.Objects;
import java.util.Optional;

@Service
public class UserService {

    private static final String FREE_PLAN = "Free";

    private final UserRepository users;
    private final PlanRepository plans;
    private final CreditService credits;
    private final TransactionTemplate tx;

    public UserService(UserRepository users, PlanRepository plans,
                       CreditService credits, TransactionTemplate tx) {
        this.users = users;
        this.plans = plans;
        this.credits = credits;
        this.tx = tx;
    }

    /**
     * Returns the user for this Telegram account, creating it (with the
     * free plan's credits) on first contact and keeping the display
     * name/username fresh afterwards.
     */
    public User registerOrUpdate(long telegramUserId, String username, String firstName) {
        Optional<User> existing = users.findByTelegramUserId(telegramUserId);
        if (existing.isPresent()) {
            return refreshProfile(existing.get(), username, firstName);
        }
        try {
            return tx.execute(status -> createWithFreePlan(telegramUserId, username, firstName));
        } catch (DataIntegrityViolationException e) {
            // Two first messages raced; the other one won. Use its row.
            return users.findByTelegramUserId(telegramUserId).orElseThrow(() -> e);
        }
    }

    private User createWithFreePlan(long telegramUserId, String username, String firstName) {
        Plan free = plans.findByNameIgnoreCase(FREE_PLAN)
                .orElseThrow(() -> new IllegalStateException(
                        "Plan '" + FREE_PLAN + "' not found - is data.sql seeding running?"));
        User user = new User();
        user.setTelegramUserId(telegramUserId);
        user.setUsername(username);
        user.setFirstName(firstName);
        user.setStatus(UserStatus.ACTIVE);
        user = users.save(user);
        credits.grantPlan(user, free);
        return user;
    }

    private User refreshProfile(User user, String username, String firstName) {
        if (Objects.equals(user.getUsername(), username) && Objects.equals(user.getFirstName(), firstName)) {
            return user; // no write on every message
        }
        user.setUsername(username);
        user.setFirstName(firstName);
        return users.save(user);
    }
}

