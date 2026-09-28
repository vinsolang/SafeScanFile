package com.backend.backend.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.backend.backend.exception.InsufficientCreditsException;
import com.backend.backend.models.Plan;
import com.backend.backend.models.Subscription;
import com.backend.backend.models.SubscriptionStatus;
import com.backend.backend.models.User;
import com.backend.backend.repository.SubscriptionRepository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scan credits live on subscriptions. A user may hold several (the free
 * plan plus purchased ones); credits are spent from the soonest-expiring
 * one first.
 */
@Service
public class CreditService {

    private final SubscriptionRepository subscriptions;

    public CreditService(SubscriptionRepository subscriptions) {
        this.subscriptions = subscriptions;
    }

    @Transactional(readOnly = true)
    public long balance(Long userId) {
        return subscriptions.sumSpendableCredits(userId, SubscriptionStatus.ACTIVE, LocalDateTime.now());
    }

    /**
     * Atomically spends one credit.
     *
     * Each candidate row is re-read with a pessimistic lock before the
     * decrement, so two simultaneous uploads from one user can never spend
     * the same credit (the second waits, then sees the reduced balance).
     *
     * @return id of the subscription that was charged, for refunds
     * @throws InsufficientCreditsException if nothing is spendable
     */
    @Transactional
    public Long consume(Long userId) {
        List<Long> candidates = subscriptions.findSpendableIds(
                userId, SubscriptionStatus.ACTIVE, LocalDateTime.now());
        for (Long id : candidates) {
            Subscription locked = (Subscription) (Object) subscriptions.findByIdForUpdate(id).orElse(null);
            if (locked != null && locked.getCredits() > 0) {
                locked.setCredits(locked.getCredits() - 1);
                return locked.getId();
            }
        }
        throw new InsufficientCreditsException();
    }

    /** Gives back a credit taken by {@link #consume}, e.g. when the scan itself failed. */
    @Transactional
    public void refund(Long subscriptionId) {
        Object candidate = subscriptions.findByIdForUpdate(subscriptionId).orElse(null);
        if (candidate != null) {
            Subscription s = (Subscription) candidate;
            s.setCredits(s.getCredits() + 1);
        }
    }

    /** Creates a subscription carrying the plan's credits (free signup, purchases). */
    @Transactional
    public Subscription grantPlan(User user, Plan plan) {
        LocalDateTime now = LocalDateTime.now();
        Subscription s = new Subscription();
        s.setUser(user);
        s.setPlan(plan);
        s.setCredits(plan.getScanLimit());
        s.setStartDate(now);
        s.setEndDate(now.plusDays(plan.getDurationDays()));
        s.setStatus(SubscriptionStatus.ACTIVE);
        return subscriptions.save(s);
    }
}
