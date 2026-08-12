package com.nilambar.erp.service.payment;

import java.math.BigDecimal;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Stand-in for a real gateway: every charge succeeds unless the amount is non-positive or
 * {@link #setForceFailure(boolean)} has been used to exercise the failure path.
 */
@Service
public class MockPaymentService implements PaymentService {

    private static final Logger log = LoggerFactory.getLogger(MockPaymentService.class);

    private volatile boolean forceFailure;

    @Override
    public PaymentResult charge(Long userId, String orderNumber, BigDecimal amount) {
        if (forceFailure) {
            log.info("Mock payment forced to fail for order {}", orderNumber);
            return PaymentResult.failure("Payment declined by mock gateway.");
        }
        if (amount == null || amount.signum() < 0) {
            return PaymentResult.failure("Invalid amount.");
        }
        String reference = "MOCKPAY-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();
        log.info("Mock payment captured {} for order {} (ref {})", amount, orderNumber, reference);
        return PaymentResult.success(reference);
    }

    @Override
    public PaymentResult refund(Long userId, String orderNumber, BigDecimal amount) {
        if (forceFailure) {
            log.info("Mock refund forced to fail for order {}", orderNumber);
            return PaymentResult.failure("Refund declined by mock gateway.");
        }
        if (amount == null || amount.signum() <= 0) {
            return PaymentResult.failure("Invalid refund amount.");
        }
        String reference = "MOCKRFND-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();
        log.info("Mock refund of {} issued for order {} (ref {})", amount, orderNumber, reference);
        return PaymentResult.success(reference);
    }

    public void setForceFailure(boolean forceFailure) {
        this.forceFailure = forceFailure;
    }
}
