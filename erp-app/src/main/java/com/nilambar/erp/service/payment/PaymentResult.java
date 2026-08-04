package com.nilambar.erp.service.payment;

public record PaymentResult(boolean successful, String reference, String failureReason) {

    public static PaymentResult success(String reference) {
        return new PaymentResult(true, reference, null);
    }

    public static PaymentResult failure(String reason) {
        return new PaymentResult(false, null, reason);
    }
}
