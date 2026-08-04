package com.nilambar.erp.service.payment;

import java.math.BigDecimal;

public interface PaymentService {

    PaymentResult charge(Long userId, String orderNumber, BigDecimal amount);
}
