package com.nilambar.erp.event;

import java.math.BigDecimal;
import java.util.List;

public record ReturnRequestedEvent(
        String eventId,
        Long returnId,
        String returnNumber,
        Long orderId,
        String orderNumber,
        Long userId,
        String reason,
        BigDecimal refundAmount,
        List<Line> lines,
        String requestedAt) {

    public record Line(Long productId, String productName, int quantity) {
    }
}
