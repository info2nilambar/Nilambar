package com.nilambar.erp.event;

import java.math.BigDecimal;
import java.util.List;

public record OrderPlacedEvent(
        String eventId,
        Long orderId,
        String orderNumber,
        Long userId,
        String fulfilmentType,
        Double distanceKm,
        BigDecimal total,
        List<Line> lines,
        String placedAt) {

    public record Line(Long productId, String productName, int quantity, BigDecimal unitPrice) {
    }
}
