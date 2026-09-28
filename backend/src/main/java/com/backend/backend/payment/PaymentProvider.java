package com.backend.backend.payment;

import java.math.BigDecimal;

/**
 * A payment gateway (Stripe, Telegram Payments, crypto, ...). Implement one
 * per gateway and register it as a Spring bean; {@link PaymentService}
 * finds it by {@link #name()}.
 */
public interface PaymentProvider {

    /** Stable identifier stored in {@code payments.provider}. */
    String name();

    /**
     * Asks the gateway itself (server-to-server) whether this transaction
     * was really paid, for at least this amount and currency. Never derive
     * this from anything the client or a webhook body merely claims.
     */
    boolean isPaid(String transactionId, BigDecimal expectedAmount, String currency);
}
