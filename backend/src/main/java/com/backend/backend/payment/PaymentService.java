package com.backend.backend.payment;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.backend.backend.exception.SafeScanException;
import com.backend.backend.models.Payment;
import com.backend.backend.models.PaymentStatus;
import com.backend.backend.models.Plan;
import com.backend.backend.models.User;
import com.backend.backend.repository.PaymentRepository;
import com.backend.backend.services.CreditService;

/**
 * Payment lifecycle. Credits are granted only from {@link #confirm}, and
 * only after the provider itself confirms the money arrived.
 *
 * No concrete provider is wired up yet (Version 3), so today this is the
 * tested-in-isolation core that a webhook controller will call.
 */
@Slf4j
@Service
public class PaymentService {

    private static final String CURRENCY = "USD";

    private final PaymentRepository payments;
    private final CreditService credits;
    private final ObjectProvider<PaymentProvider> providers;

    public PaymentService(PaymentRepository payments, CreditService credits,
                          ObjectProvider<PaymentProvider> providers) {
        this.payments = payments;
        this.credits = credits;
        this.providers = providers;
    }

    public boolean isEnabled() {
        return providers.stream().findAny().isPresent();
    }

    /** Records that the user started paying for a plan. Grants nothing yet. */
    @Transactional
    public Payment createPending(User user, Plan plan, String providerName, String transactionId) {
        Payment p = new Payment();
        p.setUser(user);
        p.setPlan(plan);
        p.setAmount(plan.getPrice());
        p.setCurrency(CURRENCY);
        p.setProvider(providerName);
        p.setTransactionId(transactionId);
        p.setStatus(PaymentStatus.PENDING);
        return payments.save(p);
    }

    /**
     * Called when a webhook says a transaction changed. Verifies with the
     * provider and, if paid, marks SUCCESS and grants the plan's credits.
     *
     * Idempotent and safe under duplicate/concurrent deliveries: the row is
     * locked, and an already-SUCCESS payment returns immediately, so credits
     * can never be granted twice for one transaction.
     *
     * @return the payment; check {@code getStatus()} - it stays PENDING if
     *         the provider doesn't (yet) report it as paid
     */
    @Transactional
    public Payment confirm(String transactionId) {
        Payment p = payments.findByTransactionId(transactionId)
                .orElseThrow(() -> new SafeScanException("Unknown transaction"));
        if (p.getStatus() == PaymentStatus.SUCCESS) {
            return p;
        }
        PaymentProvider provider = providers.stream()
                .filter(x -> x.name().equals(p.getProvider()))
                .findFirst()
                .orElseThrow(() -> new SafeScanException("No provider configured: " + p.getProvider()));

        if (!provider.isPaid(transactionId, p.getAmount(), p.getCurrency())) {
            log.info("Transaction {} not paid (yet) according to {}", transactionId, provider.name());
            return p;
        }
        p.setStatus(PaymentStatus.SUCCESS);
        credits.grantPlan(p.getUser(), p.getPlan());
        log.info("Payment {} confirmed, granted plan {} to user {}",
                p.getId(), p.getPlan().getName(), p.getUser().getId());
        return p;
    }
}
